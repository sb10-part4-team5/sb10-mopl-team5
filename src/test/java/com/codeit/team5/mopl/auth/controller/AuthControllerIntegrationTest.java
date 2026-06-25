package com.codeit.team5.mopl.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.auth.dto.request.SignInRequest;
import com.codeit.team5.mopl.auth.repository.RefreshTokenRepository;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "jwt.access-secret-key=abcdefghijklmnopqrstuvwxyz123456",
        "jwt.refresh-secret-key=123456abcdefghijklmnopqrstuvwxyz",
        "jwt.access-token-expiration-minutes=30",
        "jwt.refresh-token-expiration-minutes=420"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthControllerIntegrationTest {

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
    @DisplayName("로그인에 성공하면 accessToken을 반환하고 리프레시 토큰 해시를 저장한다")
    void login_success() throws Exception {
        // Given
        SignInRequest request = saveLoginUser("login-flow@example.com", "password1");

        // When & Then
        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.userDto.email").value(request.username()));

        assertThat(refreshTokenRepository.findAll())
                .singleElement()
                .satisfies(refreshToken -> {
                    assertThat(refreshToken.getTokenHash()).isNotBlank();
                    assertThat(refreshToken.getTokenHash()).isNotEqualTo("refresh-token");
                    assertThat(refreshToken.getTokenHash()).hasSize(64);
                });
    }

    @Test
    @DisplayName("로그아웃에 성공하면 저장된 리프레시 토큰을 삭제한다")
    void logout_success() throws Exception {
        // Given
        SignInRequest request = saveLoginUser("logout-flow@example.com", "password1");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = objectMapper
                .readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
        assertThat(refreshTokenRepository.count()).isEqualTo(1);

        // When & Then
        mockMvc.perform(post("/api/auth/sign-out")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        assertThat(refreshTokenRepository.count()).isZero();
    }

    @Test
    @DisplayName("인증 헤더 없이 로그아웃을 요청하면 실패한다")
    void logout_missingAuthorizationHeader_returnsUnauthorized() throws Exception {
        // Given: Authorization 헤더 없음

        // When & Then
        mockMvc.perform(post("/api/auth/sign-out"))
                .andExpect(status().isUnauthorized());
    }

    private SignInRequest saveLoginUser(String email, String rawPassword) {
        User user = User.create(email, passwordEncoder.encode(rawPassword), "사용자");
        userRepository.saveAndFlush(user);
        return new SignInRequest(email, rawPassword);
    }
}
