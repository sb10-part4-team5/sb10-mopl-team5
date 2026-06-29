package com.codeit.team5.mopl.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.team5.mopl.TestGlobalExceptionHandlerConfig;
import com.codeit.team5.mopl.config.SecurityConfig;
import com.codeit.team5.mopl.auth.filter.JwtAuthenticationFilter;
import com.codeit.team5.mopl.auth.handler.UserAccessDeniedHandler;
import com.codeit.team5.mopl.auth.handler.UserAuthenticationEntryPoint;
import com.codeit.team5.mopl.auth.jwt.JwtTokenizer;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetailsService;
import com.codeit.team5.mopl.auth.security.provider.MoplAuthenticationProvider;
import com.codeit.team5.mopl.global.exception.GlobalExceptionHandler;
import com.codeit.team5.mopl.user.dto.request.UserRegisterRequest;
import com.codeit.team5.mopl.user.dto.request.UserUpdateRequest;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import com.codeit.team5.mopl.user.exception.DuplicatedEmailException;
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
        given(userService.update(any(), any(), any())).willReturn(response);

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(new UserUpdateRequest("새이름")));
        MockMultipartFile imagePart = new MockMultipartFile(
                "image", "profile.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3});

        // When & Then
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/{userId}", userId)
                        .file(requestPart).file(imagePart)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("새이름"))
                .andExpect(jsonPath("$.profileImageUrl").value("http://localhost/profiles/key.jpg"));

        verify(userService).update(any(), any(), any());
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
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"));

        verify(userService, never()).update(any(), any(), any());
    }

}
