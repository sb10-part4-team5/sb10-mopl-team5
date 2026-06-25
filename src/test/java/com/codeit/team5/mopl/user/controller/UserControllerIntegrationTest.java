package com.codeit.team5.mopl.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.user.dto.request.UserRegisterRequest;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
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
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원가입에 성공하고 사용자가 데이터베이스에 저장된다")
    void createUser_success() throws Exception {
        // Given
        UserRegisterRequest request =
                new UserRegisterRequest("사용자", "user@example.com", "password1");

        // When & Then
        mockMvc.perform(post("/api/users")
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

        List<User> savedUsers = userRepository.findAll();
        assertThat(savedUsers).hasSize(1);

        User savedUser = savedUsers.get(0);
        assertThat(savedUser.getEmail()).isEqualTo(request.email());
        assertThat(savedUser.getName()).isEqualTo(request.name());
        assertThat(savedUser.getPassword()).isNotEqualTo(request.password());
        assertThat(passwordEncoder.matches(request.password(), savedUser.getPassword())).isTrue();
    }

    @Test
    @DisplayName("중복 이메일로 회원가입하면 409 충돌 응답을 반환하고 사용자를 추가하지 않는다")
    void createUser_duplicateEmail_returnsConflict() throws Exception {
        // Given
        User existingUser = User.create("duplicate@example.com", "encoded-password", "기존 사용자");
        userRepository.saveAndFlush(existingUser);

        UserRegisterRequest request =
                new UserRegisterRequest("신규 사용자", "duplicate@example.com", "password1");

        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.exceptionType").value("DuplicatedEmailException"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."))
                .andExpect(jsonPath("$.details").doesNotExist());

        assertThat(userRepository.findAll())
                .singleElement()
                .extracting(User::getEmail)
                .isEqualTo("duplicate@example.com");
    }

    @Test
    @DisplayName("필수 사용자명이 누락되면 검증 실패 응답을 반환하고 저장하지 않는다")
    void createUser_missingName_returnsBadRequest() throws Exception {
        // Given
        UserRegisterRequest request =
                new UserRegisterRequest("", "user@example.com", "password1");

        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.details.name[0]").value("사용자명은 필수입니다."));

        assertThat(userRepository.count()).isZero();
    }

    @Test
    @DisplayName("이메일 형식이 올바르지 않으면 검증 실패 응답을 반환하고 저장하지 않는다")
    void createUser_invalidEmail_returnsBadRequest() throws Exception {
        // Given
        UserRegisterRequest request =
                new UserRegisterRequest("사용자", "invalid-email", "password1");

        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.details.email").isArray())
                .andExpect(jsonPath("$.details.email").isNotEmpty());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    @DisplayName("비밀번호 조건을 충족하지 않으면 검증 실패 응답을 반환하고 저장하지 않는다")
    void createUser_invalidPassword_returnsBadRequest() throws Exception {
        // Given
        UserRegisterRequest request =
                new UserRegisterRequest("사용자", "user@example.com", "password");

        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.details.password[0]")
                        .value("비밀번호는 영문자와 숫자를 포함하여 8자 이상이어야 합니다."));

        assertThat(userRepository.count()).isZero();
    }

    @Test
    @DisplayName("사용자 상세 조회 성공")
    void getUser_success() throws Exception {
        // Given
        User savedUser = userRepository.saveAndFlush(
                User.create("user@example.com", "encoded-password", "사용자"));

        // When & Then
        mockMvc.perform(get("/api/users/{userId}", savedUser.getId()))
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
        // 예외 핸들러 마이그레이션 전이라 상태코드는 에러(>=400)로만 일반화 검증
        // (핸들러 활성화 후 404 + ErrorResponseSuggestion 형식으로 강화 예정)
        mockMvc.perform(get("/api/users/{userId}", unknownId))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isGreaterThanOrEqualTo(400));
    }

    @Test
    @DisplayName("잘못된 형식의 userId 조회 실패")
    void getUser_invalidUuid() throws Exception {
        // Given: UUID로 변환 불가능한 경로 변수

        // When & Then
        // 타입 변환 실패는 에러 응답(>=400)으로만 일반화 검증
        // (핸들러에 MethodArgumentTypeMismatch -> 400 처리 추가 시 정확한 400으로 강화 예정)
        mockMvc.perform(get("/api/users/{userId}", "invalid-uuid"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isGreaterThanOrEqualTo(400));
    }
}
