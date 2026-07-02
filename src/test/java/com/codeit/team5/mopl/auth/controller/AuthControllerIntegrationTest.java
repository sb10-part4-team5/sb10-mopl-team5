package com.codeit.team5.mopl.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.auth.dto.request.SignInRequest;
import com.codeit.team5.mopl.auth.repository.RefreshTokenRepository;
import com.codeit.team5.mopl.auth.repository.TemporaryPasswordRepository;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.mockito.ArgumentCaptor;

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
    private TemporaryPasswordRepository temporaryPasswordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JavaMailSender mailSender;

    @AfterEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        temporaryPasswordRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("로그인에 성공하면 accessToken을 반환하고 리프레시 토큰 해시를 저장한다")
    void login_success() throws Exception {
        // Given
        SignInRequest request = saveLoginUser("login-flow@example.com", "password1");

        // When & Then
        MvcResult loginResult = mockMvc.perform(post("/api/auth/sign-in")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", request.username())
                        .param("password", request.password()))
                .andExpect(status().isOk())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                        Matchers.hasItem(Matchers.allOf(
                                Matchers.containsString("REFRESH_TOKEN="),
                                Matchers.containsString("Path=/api/auth"),
                                Matchers.containsString("Max-Age=25200"),
                                Matchers.containsString("HttpOnly"),
                                Matchers.containsString("SameSite=Lax")
                        ))))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                        Matchers.not(Matchers.hasItem(Matchers.containsString("XSRF-TOKEN=")))))
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
                        .with(csrf())
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
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken)
                        .cookie(refreshTokenCookie))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        Matchers.allOf(
                                Matchers.containsString("REFRESH_TOKEN="),
                                Matchers.containsString("Path=/api/auth"),
                                Matchers.containsString("Max-Age=0"),
                                Matchers.containsString("HttpOnly"),
                                Matchers.containsString("SameSite=Lax")
                        )));

        assertThat(refreshTokenRepository.count()).isZero();
    }

    @Test
    @DisplayName("인증 헤더 없이 로그아웃을 요청하면 refresh token cookie 삭제 응답을 반환한다")
    void logout_missingAuthorizationHeader_deletesRefreshTokenCookie() throws Exception {
        // Given: Authorization 헤더와 refresh token cookie가 없음

        // When & Then
        mockMvc.perform(post("/api/auth/sign-out")
                        .with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        Matchers.allOf(
                                Matchers.containsString("REFRESH_TOKEN="),
                                Matchers.containsString("Path=/api/auth"),
                                Matchers.containsString("Max-Age=0"),
                                Matchers.containsString("HttpOnly"),
                                Matchers.containsString("SameSite=Lax")
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
    @DisplayName("비밀번호 초기화 후 임시 비밀번호로 로그인하고 새 비밀번호로 변경할 수 있다")
    void resetPassword_temporaryPasswordLoginAndChangePassword_success() throws Exception {
        // Given
        SignInRequest originalRequest = saveLoginUser("reset-flow@example.com", "password1");
        User user = userRepository.findByEmail(originalRequest.username()).orElseThrow();

        // When
        mockMvc.perform(post("/api/auth/reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "Reset-Flow@Example.COM"
                                }
                                """))
                .andExpect(status().isNoContent());

        // Then
        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        assertThat(temporaryPasswordRepository.findByUserId(user.getId())).isPresent();

        TestTransaction.flagForCommit();
        TestTransaction.end();

        verify(mailSender, timeout(5000)).send(mailCaptor.capture());
        SimpleMailMessage mail = mailCaptor.getValue();
        assertThat(mail.getTo()).containsExactly(originalRequest.username());
        assertThat(mail.getSubject()).isEqualTo("[MOPL] 임시 비밀번호 안내");
        String temporaryPassword = extractTemporaryPassword(mail.getText());
        assertThat(temporaryPassword).isNotBlank();

        MvcResult temporaryLoginResult = mockMvc.perform(post("/api/auth/sign-in")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", originalRequest.username())
                        .param("password", temporaryPassword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.userDto.email").value(originalRequest.username()))
                .andReturn();
        String temporaryAccessToken = objectMapper
                .readTree(temporaryLoginResult.getResponse().getContentAsString())
                .get("accessToken")
                .asText();

        mockMvc.perform(patch("/api/users/{userId}/password", user.getId())
                        .with(csrf())
                        .header("Authorization", "Bearer " + temporaryAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "newPassword1"
                                }
                                """))
                .andExpect(status().isNoContent());

        assertThat(temporaryPasswordRepository.findByUserId(user.getId())).isEmpty();

        mockMvc.perform(post("/api/auth/sign-in")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", originalRequest.username())
                        .param("password", "newPassword1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        mockMvc.perform(post("/api/auth/sign-in")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", originalRequest.username())
                        .param("password", originalRequest.password()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_CREDENTIALS"));

        mockMvc.perform(post("/api/auth/sign-in")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", originalRequest.username())
                        .param("password", temporaryPassword))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰이면 인증 실패 응답을 반환한다")
    void refresh_invalidRefreshToken_returnsUnauthorized() throws Exception {
        // Given
        Cookie refreshTokenCookie = new Cookie("REFRESH_TOKEN", "refresh-token");

        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                .cookie(refreshTokenCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.exceptionType").value("RefreshTokenInvalidException"))
                .andExpect(jsonPath("$.message").value("Invalid refresh token"))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("같은 계정으로 다시 로그인하면 이전 리프레시 토큰으로 재발급에 실패한다")
    void refresh_afterReLoginWithOldRefreshToken_returnsUnauthorized() throws Exception {
        // Given
        SignInRequest request = saveLoginUser("old-refresh-flow@example.com", "password1");
        Cookie oldRefreshTokenCookie = login(request).getResponse().getCookie("REFRESH_TOKEN");
        Cookie newRefreshTokenCookie = login(request).getResponse().getCookie("REFRESH_TOKEN");

        assertThat(oldRefreshTokenCookie).isNotNull();
        assertThat(newRefreshTokenCookie).isNotNull();
        assertThat(newRefreshTokenCookie.getValue()).isNotEqualTo(oldRefreshTokenCookie.getValue());
        assertThat(refreshTokenRepository.count()).isEqualTo(1);

        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                .cookie(oldRefreshTokenCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.exceptionType").value("RefreshTokenInvalidException"))
                .andExpect(jsonPath("$.message").value("Invalid refresh token"))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("같은 계정으로 다시 로그인해도 최신 리프레시 토큰으로 재발급에 성공한다")
    void refresh_afterReLoginWithLatestRefreshToken_success() throws Exception {
        // Given
        SignInRequest request = saveLoginUser("latest-refresh-flow@example.com", "password1");
        login(request);
        Cookie latestRefreshTokenCookie = login(request).getResponse().getCookie("REFRESH_TOKEN");

        assertThat(latestRefreshTokenCookie).isNotNull();
        assertThat(refreshTokenRepository.count()).isEqualTo(1);

        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(latestRefreshTokenCookie))
                .andExpect(status().isOk())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                        Matchers.hasItem(Matchers.allOf(
                                Matchers.containsString("REFRESH_TOKEN="),
                                Matchers.containsString("Path=/api/auth"),
                                Matchers.containsString("Max-Age=25200"),
                                Matchers.containsString("HttpOnly"),
                                Matchers.containsString("SameSite=Lax")
                        ))))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                        Matchers.not(Matchers.hasItem(Matchers.containsString("XSRF-TOKEN=")))))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.userDto.email").value(request.username()));
    }

    @Test
    @DisplayName("잘못된 JWT로 인증이 실패하면 AuthenticationEntryPoint 응답을 반환한다")
    void authenticatedRequest_invalidJwt_returnsEntryPointResponse() throws Exception {
        // Given
        String invalidAccessToken = "invalid-access-token";

        // When & Then
        mockMvc.perform(post("/api/follows")
                        .with(csrf())
                        .header("Authorization", "Bearer " + invalidAccessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.exceptionType").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("인증 없이 보호 API를 요청하면 AuthenticationEntryPoint 응답을 반환한다")
    void authenticatedRequest_missingAuthorization_returnsEntryPointResponse() throws Exception {
        // Given: Authorization 헤더 없음

        // When & Then
        mockMvc.perform(post("/api/follows")
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.exceptionType").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    private SignInRequest saveLoginUser(String email, String rawPassword) {
        User user = User.create(email, passwordEncoder.encode(rawPassword), "사용자");
        userRepository.saveAndFlush(user);
        return new SignInRequest(email, rawPassword);
    }

    private String extractTemporaryPassword(String mailText) {
        return mailText.lines()
                .filter(line -> line.startsWith("임시 비밀번호: "))
                .map(line -> line.substring("임시 비밀번호: ".length()).trim())
                .findFirst()
                .orElseThrow();
    }

    private MvcResult login(SignInRequest request) throws Exception {
        return mockMvc.perform(post("/api/auth/sign-in")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", request.username())
                        .param("password", request.password()))
                .andExpect(status().isOk())
                .andReturn();
    }
}
