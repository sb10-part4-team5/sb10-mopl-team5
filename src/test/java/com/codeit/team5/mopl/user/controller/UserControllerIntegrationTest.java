package com.codeit.team5.mopl.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.auth.dto.request.SignInRequest;
import com.codeit.team5.mopl.auth.repository.RefreshTokenRepository;
import com.codeit.team5.mopl.auth.security.details.AuthUser;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.user.dto.request.UserRegisterRequest;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import com.codeit.team5.mopl.user.dto.request.UserUpdateRequest;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.entity.UserRole;
import com.codeit.team5.mopl.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import java.time.Instant;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "management.health.mail.enabled=false",
        "spring.mail.host=localhost",
        "spring.mail.port=2525",
        "spring.mail.username=test",
        "spring.mail.password=test"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원가입에 성공하고 사용자가 데이터베이스에 저장된다")
    void createUser_success() throws Exception {
        // Given
        UserRegisterRequest request =
                new UserRegisterRequest("사용자", "user@example.com", "password1");

        // When & Then
        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.email").value(request.email()))
                .andExpect(jsonPath("$.name").value(request.name()))
                .andExpect(jsonPath("$.profileImageUrl").doesNotExist())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.locked").value(false));

        User savedUser = userRepository.findByEmail(request.email()).orElseThrow();
        assertThat(savedUser.getEmail()).isEqualTo(request.email());
        assertThat(savedUser.getName()).isEqualTo(request.name());
        assertThat(savedUser.getRole().name()).isEqualTo("USER");
        assertThat(savedUser.getPassword()).isNotEqualTo(request.password());
        assertThat(passwordEncoder.matches(request.password(), savedUser.getPassword())).isTrue();
    }

    @Test
    @DisplayName("중복 이메일로 회원가입하면 409 충돌 응답을 반환하고 사용자를 추가하지 않는다")
    void createUser_duplicateEmail_returnsConflict() throws Exception {
        // Given
        User existingUser = User.create("duplicate@example.com", "encoded-password", "기존 사용자");
        userRepository.saveAndFlush(existingUser);
        long userCountBeforeRequest = userRepository.count();

        UserRegisterRequest request =
                new UserRegisterRequest("신규 사용자", "duplicate@example.com", "password1");

        // When & Then
        mockMvc.perform(post("/api/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.exceptionType").value("DuplicatedEmailException"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."))
                .andExpect(jsonPath("$.details").doesNotExist());

        assertThat(userRepository.count()).isEqualTo(userCountBeforeRequest);
        User savedUser = userRepository.findByEmail("duplicate@example.com").orElseThrow();
        assertThat(savedUser.getEmail()).isEqualTo("duplicate@example.com");
        assertThat(savedUser.getName()).isEqualTo("기존 사용자");
    }

    @Test
    @DisplayName("필수 사용자명이 누락되면 검증 실패 응답을 반환하고 저장하지 않는다")
    void createUser_missingName_returnsBadRequest() throws Exception {
        // Given
        UserRegisterRequest request =
                new UserRegisterRequest("", "user@example.com", "password1");
        long userCountBeforeRequest = userRepository.count();

        // When & Then
        mockMvc.perform(post("/api/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.details.name[0]").value("사용자명은 필수입니다."));

        assertThat(userRepository.count()).isEqualTo(userCountBeforeRequest);
        assertThat(userRepository.findByEmail(request.email())).isEmpty();
    }

    @Test
    @DisplayName("이메일 형식이 올바르지 않으면 검증 실패 응답을 반환하고 저장하지 않는다")
    void createUser_invalidEmail_returnsBadRequest() throws Exception {
        // Given
        UserRegisterRequest request =
                new UserRegisterRequest("사용자", "invalid-email", "password1");
        long userCountBeforeRequest = userRepository.count();

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

        assertThat(userRepository.count()).isEqualTo(userCountBeforeRequest);
        assertThat(userRepository.findByEmail(request.email())).isEmpty();
    }

    @Test
    @DisplayName("비밀번호 조건을 충족하지 않으면 검증 실패 응답을 반환하고 저장하지 않는다")
    void createUser_invalidPassword_returnsBadRequest() throws Exception {
        // Given
        UserRegisterRequest request =
                new UserRegisterRequest("사용자", "user@example.com", "password");
        long userCountBeforeRequest = userRepository.count();

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

        assertThat(userRepository.count()).isEqualTo(userCountBeforeRequest);
        assertThat(userRepository.findByEmail(request.email())).isEmpty();
    }

    @Test
    @DisplayName("사용자 상세 조회 성공")
    void getUser_success() throws Exception {
        // Given
        User savedUser = userRepository.saveAndFlush(
                User.create("user@example.com", "encoded-password", "사용자"));

        // When & Then
        mockMvc.perform(get("/api/users/{userId}", savedUser.getId())
                        .with(authentication(authOf(UUID.randomUUID()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUser.getId().toString()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.name").value("사용자"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.locked").value(false));
    }

    @Test
    @DisplayName("존재하지 않는 사용자 조회 실패")
    void getUser_notFound() throws Exception {
        // Given
        UUID unknownId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(get("/api/users/{userId}", unknownId)
                        .with(authentication(authOf(UUID.randomUUID()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.exceptionType").value("UserNotFoundException"))
                .andExpect(jsonPath("$.message").value("사용자가 존재하지 않습니다."));
    }

    @Test
    @DisplayName("잘못된 형식의 userId 조회 실패")
    void getUser_invalidUuid() throws Exception {
        // Given: UUID로 변환 불가능한 경로 변수

        // When & Then
        mockMvc.perform(get("/api/users/{userId}", "invalid-uuid")
                        .with(authentication(authOf(UUID.randomUUID()))))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("인증 없이 사용자 조회 실패")
    void getUser_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/{userId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("프로필 이름 변경 성공")
    void updateUser_nameOnly_success() throws Exception {
        // Given
        User saved = userRepository.saveAndFlush(
                User.create("user@example.com", "encoded-password", "기존이름"));
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(new UserUpdateRequest("변경된이름")));

        // When & Then
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/{userId}", saved.getId())
                        .file(requestPart)
                        .with(csrf())
                        .with(authentication(authOf(saved.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.name").value("변경된이름"));

        User updated = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("변경된이름");
    }

    @Test
    @DisplayName("프로필 변경 시 이름 공백 검증 실패")
    void updateUser_blankName_returnsBadRequest() throws Exception {
        // Given
        User saved = userRepository.saveAndFlush(
                User.create("user@example.com", "encoded-password", "기존이름"));
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(new UserUpdateRequest("   ")));

        // When & Then
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/{userId}", saved.getId())
                        .file(requestPart)
                        .with(csrf())
                        .with(authentication(authOf(saved.getId()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"));

        User notUpdated = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(notUpdated.getName()).isEqualTo("기존이름");
    }

    @Test
    @DisplayName("본인이 아닌 사용자의 프로필 변경 실패")
    void updateUser_notOwner_returnsForbidden() throws Exception {
        User saved = userRepository.saveAndFlush(
                User.create("user@example.com", "encoded-password", "기존이름"));
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(new UserUpdateRequest("변경된이름")));

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/{userId}", saved.getId())
                        .file(requestPart)
                        .with(csrf())
                        .with(authentication(authOf(UUID.randomUUID()))))
                .andExpect(status().isForbidden());

        User notUpdated = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(notUpdated.getName()).isEqualTo("기존이름");
    }

    private Authentication authOf(UUID userId) {
        UserResponse dto = new UserResponse(
                userId, Instant.now(), "user@example.com", "유저", null, "USER", false);
        MoplUserDetails details = new MoplUserDetails(new AuthUser(dto.id(), dto.email(), dto.role(), dto.locked()), "password");
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }

    @Test
    @DisplayName("관리자가 사용자 목록을 조회하면 커서 응답을 반환한다")
    void getUsers_byAdmin_success() throws Exception {
        // Given
        SignInRequest adminRequest = saveLoginUser("list-admin@example.com", "password1", UserRole.ADMIN, false);
        User first = saveUser("list-target-a@example.com", "password1", UserRole.USER, false);
        saveUser("list-target-b@example.com", "password1", UserRole.USER, true);
        String adminAccessToken = login(adminRequest);

        // When & Then
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .param("emailLike", "list-target")
                        .param("roleEqual", "USER")
                        .param("limit", "1")
                        .param("sortDirection", "ASCENDING")
                        .param("sortBy", "email"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(first.getId().toString()))
                .andExpect(jsonPath("$.data[0].email").value("list-target-a@example.com"))
                .andExpect(jsonPath("$.data[0].name").value("사용자"))
                .andExpect(jsonPath("$.data[0].role").value("USER"))
                .andExpect(jsonPath("$.data[0].locked").value(false))
                .andExpect(jsonPath("$.nextCursor").value("list-target-a@example.com"))
                .andExpect(jsonPath("$.nextIdAfter").value(first.getId().toString()))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.sortBy").value("email"))
                .andExpect(jsonPath("$.sortDirection").value("ASCENDING"));
    }

    @Test
    @DisplayName("일반 사용자가 사용자 목록을 조회하면 403 접근 거부 응답을 반환한다")
    void getUsers_byUser_returnsForbidden() throws Exception {
        // Given
        SignInRequest userRequest = saveLoginUser("list-user@example.com", "password1", UserRole.USER, false);
        String accessToken = login(userRequest);

        // When & Then
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("limit", "10")
                        .param("sortDirection", "ASCENDING")
                        .param("sortBy", "name"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.exceptionType").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("인증 없이 사용자 목록을 조회하면 401 인증 실패 응답을 반환한다")
    void getUsers_unauthenticated_returnsUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/users")
                        .param("limit", "10")
                        .param("sortDirection", "ASCENDING")
                        .param("sortBy", "name"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.exceptionType").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("관리자가 사용자 권한을 변경하면 DB 권한이 변경되고 토큰이 무효화된다")
    void updateRole_byAdmin_success() throws Exception {
        // Given
        SignInRequest adminRequest = saveLoginUser("role-admin@example.com", "password1", UserRole.ADMIN, false);
        User target = saveUser("role-target@example.com", "password1", UserRole.USER, false);
        login(new SignInRequest(target.getEmail(), "password1"));

        assertThat(refreshTokenRepository.findByUser_Id(target.getId())).isPresent();

        String adminAccessToken = login(adminRequest);
        String requestJson = """
                {
                  "role": "ADMIN"
                }
                """;

        // When & Then
        mockMvc.perform(patch("/api/users/{userId}/role", target.getId())
                        .with(csrf())
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNoContent());

        User updated = userRepository.findById(target.getId()).orElseThrow();
        assertThat(updated.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(refreshTokenRepository.findByUser_Id(target.getId())).isEmpty();
    }

    @Test
    @DisplayName("일반 사용자가 권한 변경 요청하면 403 접근 거부 응답을 반환한다")
    void updateRole_byUser_returnsForbidden() throws Exception {
        // Given
        SignInRequest userRequest = saveLoginUser("role-user@example.com", "password1", UserRole.USER, false);
        User target = saveUser("role-user-target@example.com", "password1", UserRole.USER, false);
        String accessToken = login(userRequest);

        // When & Then
        mockMvc.perform(patch("/api/users/{userId}/role", target.getId())
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.exceptionType").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("인증 없이 권한 변경 요청하면 401 인증 실패 응답을 반환한다")
    void updateRole_unauthenticated_returnsUnauthorized() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(patch("/api/users/{userId}/role", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.exceptionType").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 권한 변경 요청하면 404 응답을 반환한다")
    void updateRole_notFound_returnsNotFound() throws Exception {
        // Given
        SignInRequest adminRequest =
                saveLoginUser("role-not-found-admin@example.com", "password1", UserRole.ADMIN, false);
        String adminAccessToken = login(adminRequest);
        UUID unknownId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(patch("/api/users/{userId}/role", unknownId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.exceptionType").value("UserNotFoundException"))
                .andExpect(jsonPath("$.message").value("사용자가 존재하지 않습니다."))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("동일한 권한으로 변경 요청하면 409 충돌 응답을 반환한다")
    void updateRole_sameRole_returnsConflict() throws Exception {
        // Given
        SignInRequest adminRequest = saveLoginUser("role-same-admin@example.com", "password1", UserRole.ADMIN, false);
        User target = saveUser("role-same-target@example.com", "password1", UserRole.USER, false);
        String adminAccessToken = login(adminRequest);

        // When & Then
        mockMvc.perform(patch("/api/users/{userId}/role", target.getId())
                        .with(csrf())
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "USER"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.exceptionType").value("SameRoleAssignmentException"))
                .andExpect(jsonPath("$.message")
                        .value("현재 사용자의 역할과 변경할 역할이 동일합니다."))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("관리자가 사용자 계정을 잠그면 DB 잠금 상태가 변경되고 토큰이 무효화된다")
    void updateLock_byAdmin_success() throws Exception {
        // Given
        SignInRequest adminRequest = saveLoginUser("lock-admin@example.com", "password1", UserRole.ADMIN, false);
        User target = saveUser("lock-target@example.com", "password1", UserRole.USER, false);
        login(new SignInRequest(target.getEmail(), "password1"));

        assertThat(refreshTokenRepository.findByUser_Id(target.getId())).isPresent();

        String adminAccessToken = login(adminRequest);

        // When & Then
        mockMvc.perform(patch("/api/users/{userId}/locked", target.getId())
                        .with(csrf())
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "locked": true
                                }
                                """))
                .andExpect(status().isNoContent());

        User updated = userRepository.findById(target.getId()).orElseThrow();
        assertThat(updated.isLocked()).isTrue();
        assertThat(refreshTokenRepository.findByUser_Id(target.getId())).isEmpty();
    }

    @Test
    @DisplayName("관리자가 사용자 계정 잠금을 해제하면 DB 잠금 상태가 false로 변경된다")
    void updateLock_unlockByAdmin_success() throws Exception {
        // Given
        SignInRequest adminRequest = saveLoginUser("unlock-admin@example.com", "password1", UserRole.ADMIN, false);
        User target = saveUser("unlock-target@example.com", "password1", UserRole.USER, true);
        String adminAccessToken = login(adminRequest);

        // When & Then
        mockMvc.perform(patch("/api/users/{userId}/locked", target.getId())
                        .with(csrf())
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "locked": false
                                }
                                """))
                .andExpect(status().isNoContent());

        User updated = userRepository.findById(target.getId()).orElseThrow();
        assertThat(updated.isLocked()).isFalse();
    }

    @Test
    @DisplayName("일반 사용자가 계정 잠금 상태 변경 요청하면 403 응답을 반환한다")
    void updateLock_byUser_returnsForbidden() throws Exception {
        // Given
        SignInRequest userRequest = saveLoginUser("lock-user@example.com", "password1", UserRole.USER, false);
        User target = saveUser("lock-user-target@example.com", "password1", UserRole.USER, false);
        String accessToken = login(userRequest);

        // When & Then
        mockMvc.perform(patch("/api/users/{userId}/locked", target.getId())
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "locked": true
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.exceptionType").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("인증 없이 계정 잠금 상태 변경 요청하면 401 인증 실패 응답을 반환한다")
    void updateLock_unauthenticated_returnsUnauthorized() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(patch("/api/users/{userId}/locked", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "locked": true
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.exceptionType").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 계정 잠금 상태 변경 요청하면 404 응답을 반환한다")
    void updateLock_notFound_returnsNotFound() throws Exception {
        // Given
        SignInRequest adminRequest =
                saveLoginUser("lock-not-found-admin@example.com", "password1", UserRole.ADMIN, false);
        String adminAccessToken = login(adminRequest);
        UUID unknownId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(patch("/api/users/{userId}/locked", unknownId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "locked": true
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.exceptionType").value("UserNotFoundException"))
                .andExpect(jsonPath("$.message").value("사용자가 존재하지 않습니다."))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("동일한 계정 잠금 상태로 변경 요청하면 409 충돌 응답을 반환한다")
    void updateLock_sameStatus_returnsConflict() throws Exception {
        // Given
        SignInRequest adminRequest = saveLoginUser("lock-same-admin@example.com", "password1", UserRole.ADMIN, false);
        User target = saveUser("lock-same-target@example.com", "password1", UserRole.USER, false);
        String adminAccessToken = login(adminRequest);

        // When & Then
        mockMvc.perform(patch("/api/users/{userId}/locked", target.getId())
                        .with(csrf())
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "locked": false
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.exceptionType").value("SameLockStatusException"))
                .andExpect(jsonPath("$.message")
                        .value("현재 사용자의 잠금 상태와 변경할 잠금 상태가 동일합니다."))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("잠긴 사용자는 로그인할 수 없다")
    void login_lockedUser_returnsForbidden() throws Exception {
        // Given
        SignInRequest request = saveLoginUser("locked-login@example.com", "password1", UserRole.USER, true);

        // When & Then
        mockMvc.perform(post("/api/auth/sign-in")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", request.username())
                        .param("password", request.password()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.exceptionType").value("ACCOUNT_LOCKED"))
                .andExpect(jsonPath("$.message").value("잠긴 계정입니다."))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    private SignInRequest saveLoginUser(String email, String rawPassword, UserRole role, boolean locked) {
        saveUser(email, rawPassword, role, locked);
        return new SignInRequest(email, rawPassword);
    }

    private User saveUser(String email, String rawPassword, UserRole role, boolean locked) {
        User user = User.create(email, passwordEncoder.encode(rawPassword), "사용자");
        if (role == UserRole.ADMIN) {
            user.updateRole(UserRole.ADMIN);
        }
        if (locked) {
            user.updateLocked(true);
        }
        return userRepository.saveAndFlush(user);
    }

    private String login(SignInRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/sign-in")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", request.username())
                        .param("password", request.password()))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
    }
}
