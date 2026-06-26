package com.codeit.team5.mopl.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.auth.dto.request.SignInRequest;
import com.codeit.team5.mopl.auth.repository.RefreshTokenRepository;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
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
        MvcResult loginResult = mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", request.username())
                        .param("password", request.password()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        Matchers.allOf(
                                Matchers.containsString("REFRESH_TOKEN="),
                                Matchers.containsString("Path=/api/auth"),
                                Matchers.containsString("Max-Age=25200"),
                                Matchers.containsString("HttpOnly")
                        )))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.userDto.email").value(request.username()))
                .andReturn();
        String refreshToken = loginResult.getResponse().getCookie("REFRESH_TOKEN").getValue();

        assertThat(refreshTokenRepository.findAll())
                .singleElement()
                .satisfies(savedRefreshToken -> {
                    assertThat(savedRefreshToken.getTokenHash()).isNotBlank();
                    assertThat(savedRefreshToken.getTokenHash()).isNotEqualTo("refresh-token");
                    assertThat(savedRefreshToken.getTokenHash()).isNotEqualTo(refreshToken);
                    assertThat(savedRefreshToken.getTokenHash()).hasSize(64);
                });
    }

    @Test
    @DisplayName("로그아웃에 성공하면 저장된 리프레시 토큰을 삭제한다")
    void logout_success() throws Exception {
        // Given
        SignInRequest request = saveLoginUser("logout-flow@example.com", "password1");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", request.username())
                        .param("password", request.password()))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = objectMapper
                .readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
        Cookie refreshTokenCookie = loginResult.getResponse().getCookie("REFRESH_TOKEN");
        assertThat(refreshTokenRepository.count()).isEqualTo(1);

        // When & Then
        mockMvc.perform(post("/api/auth/sign-out")
                        .header("Authorization", "Bearer " + accessToken)
                        .cookie(refreshTokenCookie))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        Matchers.allOf(
                                Matchers.containsString("REFRESH_TOKEN="),
                                Matchers.containsString("Path=/api/auth"),
                                Matchers.containsString("Max-Age=0"),
                                Matchers.containsString("HttpOnly")
                        )));

        assertThat(refreshTokenRepository.count()).isZero();
    }

    @Test
    @DisplayName("인증 헤더 없이 로그아웃을 요청하면 refresh token cookie 삭제 응답을 반환한다")
    void logout_missingAuthorizationHeader_deletesRefreshTokenCookie() throws Exception {
        // Given: Authorization 헤더와 refresh token cookie가 없음

        // When & Then
        mockMvc.perform(post("/api/auth/sign-out"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        Matchers.allOf(
                                Matchers.containsString("REFRESH_TOKEN="),
                                Matchers.containsString("Path=/api/auth"),
                                Matchers.containsString("Max-Age=0"),
                                Matchers.containsString("HttpOnly")
                        )));
    }

    @Test
    @DisplayName("CSRF 토큰 발급 요청에 성공하면 XSRF-TOKEN 쿠키를 반환한다")
    void csrfToken_success() throws Exception {
        // Given: CSRF 토큰 발급 엔드포인트

        // When & Then
        mockMvc.perform(get("/api/auth/csrf-token"))
                .andExpect(status().isNoContent())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                        Matchers.hasItem(Matchers.containsString("XSRF-TOKEN="))));
    }

    @Test
    @DisplayName("토큰 재발급 요청에 CSRF 토큰이 없으면 403 응답을 반환한다")
    void refresh_missingCsrf_returnsForbidden() throws Exception {
        // Given
        Cookie refreshTokenCookie = new Cookie("REFRESH_TOKEN", "refresh-token");

        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshTokenCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.exceptionType").value("FORBIDDEN"))
                .andExpect(jsonPath("$.exceptionName").doesNotExist())
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."));
    }

    @Test
    @DisplayName("잘못된 JWT로 인증이 실패하면 AuthenticationEntryPoint 응답을 반환한다")
    void authenticatedRequest_invalidJwt_returnsEntryPointResponse() throws Exception {
        // Given
        String invalidAccessToken = "invalid-access-token";

        // When & Then
        mockMvc.perform(post("/api/follows")
                        .header("Authorization", "Bearer " + invalidAccessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.exceptionType").exists())
                .andExpect(jsonPath("$.exceptionName").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    private SignInRequest saveLoginUser(String email, String rawPassword) {
        User user = User.create(email, passwordEncoder.encode(rawPassword), "사용자");
        userRepository.saveAndFlush(user);
        return new SignInRequest(email, rawPassword);
    }
}
