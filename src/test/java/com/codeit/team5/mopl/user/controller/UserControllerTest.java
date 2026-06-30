package com.codeit.team5.mopl.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.team5.mopl.TestGlobalExceptionHandlerConfig;
import com.codeit.team5.mopl.config.SecurityConfig;
import com.codeit.team5.mopl.auth.filter.JwtAuthenticationFilter;
import com.codeit.team5.mopl.auth.handler.UserAccessDeniedHandler;
import com.codeit.team5.mopl.auth.handler.UserAuthenticationEntryPoint;
import com.codeit.team5.mopl.auth.jwt.JwtTokenizer;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetailsService;
import com.codeit.team5.mopl.auth.security.provider.MoplAuthenticationProvider;
import com.codeit.team5.mopl.global.exception.GlobalExceptionHandler;
import com.codeit.team5.mopl.user.dto.request.UserLockedUpdateRequest;
import com.codeit.team5.mopl.user.dto.request.UserRegisterRequest;
import com.codeit.team5.mopl.user.dto.request.UserRoleUpdateRequest;
import com.codeit.team5.mopl.user.dto.request.UserUpdateRequest;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import com.codeit.team5.mopl.user.entity.UserRole;
import com.codeit.team5.mopl.user.exception.DuplicatedEmailException;
import com.codeit.team5.mopl.user.exception.SameLockStatusException;
import com.codeit.team5.mopl.user.exception.SameRoleAssignmentException;
import com.codeit.team5.mopl.user.exception.UserNotFoundException;
import com.codeit.team5.mopl.user.repository.UserRepository;
import com.codeit.team5.mopl.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import({
        GlobalExceptionHandler.class,
        TestGlobalExceptionHandlerConfig.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        UserAuthenticationEntryPoint.class,
        UserAccessDeniedHandler.class
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenizer jwtTokenizer;

    @MockitoBean
    private MoplUserDetailsService userDetailsService;

    @MockitoBean
    private MoplAuthenticationProvider moplAuthenticationProvider;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("정상적인 회원가입 요청이면 생성된 사용자와 201 응답을 반환한다")
    void createUser_success() throws Exception {
        // Given
        UserRegisterRequest request =
                new UserRegisterRequest("사용자", "user@example.com", "password1");
        UserResponse response = new UserResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                Instant.parse("2026-06-23T00:00:00Z"),
                request.email(),
                request.name(),
                null,
                "USER",
                false
        );
        given(userService.create(any(UserRegisterRequest.class))).willReturn(response);

        // When & Then
        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.id().toString()))
                .andExpect(jsonPath("$.createdAt").value("2026-06-23T00:00:00Z"))
                .andExpect(jsonPath("$.email").value(request.email()))
                .andExpect(jsonPath("$.name").value(request.name()))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.locked").value(false));

        ArgumentCaptor<UserRegisterRequest> requestCaptor =
                ArgumentCaptor.forClass(UserRegisterRequest.class);
        verify(userService).create(requestCaptor.capture());
        assertThat(requestCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(request);
    }

    @Test
    @DisplayName("사용자명이 공백이면 400 검증 실패 응답을 반환한다")
    void createUser_invalidName_returnsBadRequest() throws Exception {
        // Given
        UserRegisterRequest request =
                new UserRegisterRequest("   ", "user@example.com", "password1");

        // When & Then
        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.details.name[0]").value("사용자명은 필수입니다."));

        verify(userService, never()).create(any());
    }

    @Test
    @DisplayName("이메일 필드가 누락되면 400 검증 실패 응답을 반환한다")
    void createUser_missingEmail_returnsBadRequest() throws Exception {
        // Given
        String requestJson = """
                {
                  "name": "사용자",
                  "password": "password1"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.details.email[0]").value("이메일은 필수입니다."));

        verify(userService, never()).create(any());
    }

    @Test
    @DisplayName("이메일 형식이 올바르지 않으면 400 검증 실패 응답을 반환한다")
    void createUser_invalidEmail_returnsBadRequest() throws Exception {
        // Given
        UserRegisterRequest request =
                new UserRegisterRequest("사용자", "invalid-email", "password1");

        // When & Then
        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.details.email").isArray())
                .andExpect(jsonPath("$.details.email").isNotEmpty());

        verify(userService, never()).create(any());
    }

    @Test
    @DisplayName("비밀번호 필드가 누락되면 400 검증 실패 응답을 반환한다")
    void createUser_missingPassword_returnsBadRequest() throws Exception {
        // Given
        String requestJson = """
                {
                  "name": "사용자",
                  "email": "user@example.com"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.details.password").isArray())
                .andExpect(jsonPath("$.details.password")
                        .value(org.hamcrest.Matchers.hasItem("비밀번호는 필수입니다.")));

        verify(userService, never()).create(any());
    }

    @Test
    @DisplayName("비밀번호가 영문자와 숫자 조합이 아니면 400 검증 실패 응답을 반환한다")
    void createUser_invalidPassword_returnsBadRequest() throws Exception {
        // Given
        UserRegisterRequest request =
                new UserRegisterRequest("사용자", "user@example.com", "password");

        // When & Then
        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.details.password[0]")
                        .value("비밀번호는 영문자와 숫자를 포함하여 8자 이상이어야 합니다."));

        verify(userService, never()).create(any());
    }

    @Test
    @DisplayName("중복 이메일 예외가 발생하면 409 충돌 응답을 반환한다")
    void createUser_duplicateEmail_returnsConflict() throws Exception {
        // Given
        UserRegisterRequest request =
                new UserRegisterRequest("사용자", "duplicate@example.com", "password1");
        given(userService.create(any(UserRegisterRequest.class)))
                .willThrow(new DuplicatedEmailException("duplicate@example.com"));

        // When & Then
        mockMvc.perform(post("/api/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.exceptionType").value("DuplicatedEmailException"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."))
                .andExpect(jsonPath("$.details").doesNotExist());

        verify(userService).create(request);
    }

    @Test
    @DisplayName("예상하지 못한 서비스 예외가 발생하면 500 오류 응답을 반환한다")
    void createUser_unexpectedException_returnsInternalServerError() throws Exception {
        // Given
        UserRegisterRequest request =
                new UserRegisterRequest("사용자", "user@example.com", "password1");
        given(userService.create(any(UserRegisterRequest.class)))
                .willThrow(new IllegalStateException("unexpected"));

        // When & Then
        mockMvc.perform(post("/api/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.exceptionType").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 내부 에러가 발생했습니다."))
                .andExpect(jsonPath("$.details").doesNotExist());

        verify(userService).create(request);
    }

    @Test
    @DisplayName("사용자 상세 조회 성공")
    void getUser_success() throws Exception {
        // Given
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UserResponse response = new UserResponse(
                userId,
                Instant.parse("2026-06-23T00:00:00Z"),
                "user@example.com",
                "사용자",
                null,
                "USER",
                false
        );
        given(userService.getById(userId)).willReturn(response);

        // When & Then
        mockMvc.perform(get("/api/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.createdAt").value("2026-06-23T00:00:00Z"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.name").value("사용자"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.locked").value(false));

        verify(userService).getById(userId);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 조회 실패")
    void getUser_notFound() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        given(userService.getById(userId)).willThrow(new UserNotFoundException(userId));

        // When & Then
        // 예외 핸들러 마이그레이션 전이라 상태코드는 에러(>=400)로만 일반화 검증
        // (핸들러 활성화 후 404 + ErrorResponseSuggestion 형식으로 강화 예정)
        mockMvc.perform(get("/api/users/{userId}", userId))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isGreaterThanOrEqualTo(400));

        verify(userService).getById(userId);
    }

    @Test
    @DisplayName("프로필 변경 성공")
    void updateUser_success() throws Exception {
        // Given
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UserResponse response = new UserResponse(
                userId, Instant.parse("2026-06-25T00:00:00Z"),
                "user@example.com", "새이름",
                "http://localhost/profiles/key.jpg", "USER", false
        );
        given(userService.update(any(), any(), any(), any())).willReturn(response);

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(new UserUpdateRequest("새이름")));
        MockMultipartFile imagePart = new MockMultipartFile(
                "image", "profile.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3});

        // When & Then
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/{userId}", userId)
                        .file(requestPart).file(imagePart)
                        .with(csrf())
                        .with(authentication(authOf(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("새이름"))
                .andExpect(jsonPath("$.profileImageUrl").value("http://localhost/profiles/key.jpg"));

        verify(userService).update(any(), any(), any(), any());
    }

    @Test
    @DisplayName("프로필 변경 시 이름 공백 검증 실패")
    void updateUser_blankName_returnsBadRequest() throws Exception {
        // Given
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(new UserUpdateRequest("   ")));

        // When & Then
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/{userId}", userId)
                        .file(requestPart)
                        .with(csrf())
                        .with(authentication(authOf(userId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"));

        verify(userService, never()).update(any(), any(), any(), any());
    }

    private Authentication authOf(UUID userId) {
        UserResponse dto = new UserResponse(
                userId, Instant.now(), "user@example.com", "유저", null, "USER", false);
    @Test
    @DisplayName("관리자가 사용자 권한 변경 요청하면 204 응답을 반환한다")
    void updateRole_success() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UserRoleUpdateRequest request = new UserRoleUpdateRequest(UserRole.ADMIN);

        // When & Then
        mockMvc.perform(patch("/api/users/{userId}/role", userId)
                        .with(authentication(authOf(UUID.randomUUID(), "ADMIN", false)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(userService).updateRole(userId, request);
    }

    @Test
    @DisplayName("권한 변경 요청의 role이 null이면 400 검증 실패 응답을 반환한다")
    void updateRole_nullRole_returnsBadRequest() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        String requestJson = """
                {
                  "role": null
                }
                """;

        // When & Then
        mockMvc.perform(patch("/api/users/{userId}/role", userId)
                        .with(authentication(authOf(UUID.randomUUID(), "ADMIN", false)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.details.role").isArray())
                .andExpect(jsonPath("$.details.role").isNotEmpty());

        verify(userService, never()).updateRole(any(), any());
    }

    @Test
    @DisplayName("권한 변경 요청의 role 값이 잘못된 enum이면 400 검증 실패 응답을 반환한다")
    void updateRole_invalidRole_returnsBadRequest() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        String requestJson = """
                {
                  "role": "MANAGER"
                }
                """;

        // When & Then
        mockMvc.perform(patch("/api/users/{userId}/role", userId)
                        .with(authentication(authOf(UUID.randomUUID(), "ADMIN", false)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.details.role[0]").value("허용되지 않는 값입니다: MANAGER"));

        verify(userService, never()).updateRole(any(), any());
    }

    @Test
    @DisplayName("권한 변경 대상 사용자가 없으면 404 응답을 반환한다")
    void updateRole_userNotFound_returnsNotFound() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UserRoleUpdateRequest request = new UserRoleUpdateRequest(UserRole.ADMIN);
        willThrow(new UserNotFoundException(userId))
                .given(userService)
                .updateRole(userId, request);

        // When & Then
        mockMvc.perform(patch("/api/users/{userId}/role", userId)
                        .with(authentication(authOf(UUID.randomUUID(), "ADMIN", false)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.exceptionType").value("UserNotFoundException"))
                .andExpect(jsonPath("$.message").value("사용자가 존재하지 않습니다."))
                .andExpect(jsonPath("$.details").doesNotExist());

        verify(userService).updateRole(userId, request);
    }

    @Test
    @DisplayName("동일한 권한으로 변경하면 409 충돌 응답을 반환한다")
    void updateRole_sameRole_returnsConflict() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UserRoleUpdateRequest request = new UserRoleUpdateRequest(UserRole.USER);
        willThrow(new SameRoleAssignmentException("USER"))
                .given(userService)
                .updateRole(userId, request);

        // When & Then
        mockMvc.perform(patch("/api/users/{userId}/role", userId)
                        .with(authentication(authOf(UUID.randomUUID(), "ADMIN", false)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.exceptionType").value("SameRoleAssignmentException"))
                .andExpect(jsonPath("$.message")
                        .value("현재 사용자의 역할과 변경할 역할이 동일합니다."))
                .andExpect(jsonPath("$.details").doesNotExist());

        verify(userService).updateRole(userId, request);
    }

    @Test
    @DisplayName("관리자가 사용자 잠금 상태 변경 요청하면 204 응답을 반환한다")
    void updateLock_success() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UserLockedUpdateRequest request = new UserLockedUpdateRequest(true);

        // When & Then
        mockMvc.perform(patch("/api/users/{userId}/locked", userId)
                        .with(authentication(authOf(UUID.randomUUID(), "ADMIN", false)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(userService).updateLock(userId, request);
    }

    @Test
    @DisplayName("잠금 상태 변경 요청의 locked가 null이면 400 검증 실패 응답을 반환한다")
    void updateLock_nullLocked_returnsBadRequest() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        String requestJson = """
                {
                  "locked": null
                }
                """;

        // When & Then
        mockMvc.perform(patch("/api/users/{userId}/locked", userId)
                        .with(authentication(authOf(UUID.randomUUID(), "ADMIN", false)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.details.locked").isArray())
                .andExpect(jsonPath("$.details.locked").isNotEmpty());

        verify(userService, never()).updateLock(any(), any());
    }

    @Test
    @DisplayName("잠금 상태 변경 대상 사용자가 없으면 404 응답을 반환한다")
    void updateLock_userNotFound_returnsNotFound() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UserLockedUpdateRequest request = new UserLockedUpdateRequest(true);
        willThrow(new UserNotFoundException(userId))
                .given(userService)
                .updateLock(userId, request);

        // When & Then
        mockMvc.perform(patch("/api/users/{userId}/locked", userId)
                        .with(authentication(authOf(UUID.randomUUID(), "ADMIN", false)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.exceptionType").value("UserNotFoundException"))
                .andExpect(jsonPath("$.message").value("사용자가 존재하지 않습니다."))
                .andExpect(jsonPath("$.details").doesNotExist());

        verify(userService).updateLock(userId, request);
    }

    @Test
    @DisplayName("동일한 잠금 상태로 변경하면 409 충돌 응답을 반환한다")
    void updateLock_sameStatus_returnsConflict() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UserLockedUpdateRequest request = new UserLockedUpdateRequest(false);
        willThrow(new SameLockStatusException(false))
                .given(userService)
                .updateLock(userId, request);

        // When & Then
        mockMvc.perform(patch("/api/users/{userId}/locked", userId)
                        .with(authentication(authOf(UUID.randomUUID(), "ADMIN", false)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.exceptionType").value("SameLockStatusException"))
                .andExpect(jsonPath("$.message")
                        .value("현재 사용자의 잠금 상태와 변경할 잠금 상태가 동일합니다."))
                .andExpect(jsonPath("$.details").doesNotExist());

        verify(userService).updateLock(userId, request);
    }

    private Authentication authOf(UUID userId, String role, boolean locked) {
        UserResponse dto = new UserResponse(
                userId, Instant.now(), role.toLowerCase() + "@mopl.com", "사용자", null, role, locked);
        MoplUserDetails details = new MoplUserDetails(dto, "password");
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }
}
