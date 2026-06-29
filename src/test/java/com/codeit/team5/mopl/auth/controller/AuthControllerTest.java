package com.codeit.team5.mopl.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.team5.mopl.auth.cookie.RefreshTokenCookieManager;
import com.codeit.team5.mopl.auth.dto.request.SignInRequest;
import com.codeit.team5.mopl.auth.dto.response.JwtResponse;
import com.codeit.team5.mopl.auth.exception.InvalidCredentialsException;
import com.codeit.team5.mopl.auth.exception.RefreshTokenInvalidException;
import com.codeit.team5.mopl.auth.filter.JwtAuthenticationFilter;
import com.codeit.team5.mopl.auth.handler.UserAccessDeniedHandler;
import com.codeit.team5.mopl.auth.handler.UserAuthenticationEntryPoint;
import com.codeit.team5.mopl.auth.jwt.JwtTokenizer;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetailsService;
import com.codeit.team5.mopl.auth.security.provider.MoplAuthenticationProvider;
import com.codeit.team5.mopl.auth.service.AuthService;
import com.codeit.team5.mopl.auth.service.model.AuthPayload;
import com.codeit.team5.mopl.config.SecurityConfig;
import com.codeit.team5.mopl.global.exception.GlobalExceptionHandler;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.MalformedJwtException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        UserAuthenticationEntryPoint.class,
        UserAccessDeniedHandler.class
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RefreshTokenCookieManager cookieManager;

    @MockitoBean
    private JwtTokenizer jwtTokenizer;

    @MockitoBean
    private MoplUserDetailsService userDetailsService;

    @MockitoBean
    private MoplAuthenticationProvider moplAuthenticationProvider;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("로그인에 성공하면 accessToken과 사용자 정보를 반환한다")
    void login_success() throws Exception {
        // Given
        SignInRequest request = new SignInRequest("user@example.com", "password1");
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        JwtResponse response = new JwtResponse(
                new UserResponse(
                        userId,
                        Instant.parse("2026-06-24T00:00:00Z"),
                        request.username(),
                        "사용자",
                        null,
                        "USER",
                        false
                ),
                "access-token"
        );
        AuthPayload authPayload = new AuthPayload(response, "refresh-token");
        ResponseCookie refreshTokenCookie = ResponseCookie.from("REFRESH_TOKEN", "refresh-token")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(420 * 60)
                .build();
        given(authService.login(any(SignInRequest.class))).willReturn(authPayload);
        given(cookieManager.createCookie("refresh-token")).willReturn(refreshTokenCookie);

        // When & Then
        mockMvc.perform(post("/api/auth/sign-in")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", request.username())
                        .param("password", request.password()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("REFRESH_TOKEN=refresh-token"),
                                org.hamcrest.Matchers.containsString("Path=/api/auth"),
                                org.hamcrest.Matchers.containsString("Max-Age=25200"),
                                org.hamcrest.Matchers.containsString("HttpOnly"),
                                org.hamcrest.Matchers.containsString("SameSite=Lax")
                        )))
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.userDto.id").value(userId.toString()))
                .andExpect(jsonPath("$.userDto.email").value(request.username()))
                .andExpect(jsonPath("$.userDto.name").value("사용자"))
                .andExpect(jsonPath("$.userDto.role").value("USER"))
                .andExpect(jsonPath("$.userDto.locked").value(false));

        verify(authService).login(request);
        verify(cookieManager).createCookie("refresh-token");
    }

    @Test
    @DisplayName("CSRF 토큰 없이 로그인 요청하면 403 접근 거부 응답을 반환한다")
    void login_missingCsrf_returnsForbidden() throws Exception {
        // Given
        SignInRequest request = new SignInRequest("user@example.com", "password1");

        // When & Then
        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", request.username())
                        .param("password", request.password()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.exceptionType").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."))
                .andExpect(jsonPath("$.details").doesNotExist());

        verify(authService, never()).login(any());
    }

    @Test
    @DisplayName("로그인 요청에서 이메일이 누락되면 400 검증 실패 응답을 반환한다")
    void login_missingEmail_returnsBadRequest() throws Exception {
        // Given: username 파라미터가 없음

        // When & Then
        mockMvc.perform(post("/api/auth/sign-in")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("password", "password1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.details.username")
                        .value(org.hamcrest.Matchers.hasItem("이메일은 필수입니다.")));

        verify(authService, never()).login(any());
    }

    @Test
    @DisplayName("로그인 요청의 이메일 형식이 올바르지 않으면 400 검증 실패 응답을 반환한다")
    void signIn_invalidEmail_returnsBadRequest() throws Exception {
        // Given
        SignInRequest request = new SignInRequest("invalid-email", "password1");

        // When & Then
        mockMvc.perform(post("/api/auth/sign-in")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", request.username())
                        .param("password", request.password()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.details.username").isArray())
                .andExpect(jsonPath("$.details.username").isNotEmpty());

        verify(authService, never()).login(any());
    }

    @Test
    @DisplayName("로그인 요청의 이메일이 공백이면 400 검증 실패 응답을 반환한다")
    void login_blankEmail_returnsBadRequest() throws Exception {
        // Given
        SignInRequest request = new SignInRequest("   ", "password1");

        // When & Then
        mockMvc.perform(post("/api/auth/sign-in")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", request.username())
                        .param("password", request.password()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.details.username")
                        .value(org.hamcrest.Matchers.hasItem("이메일은 필수입니다.")));

        verify(authService, never()).login(any());
    }

    @Test
    @DisplayName("로그인 요청에서 비밀번호가 누락되면 400 검증 실패 응답을 반환한다")
    void login_missingPassword_returnsBadRequest() throws Exception {
        // Given: password 파라미터가 없음

        // When & Then
        mockMvc.perform(post("/api/auth/sign-in")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "user@example.com"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.details.password")
                        .value(org.hamcrest.Matchers.hasItem("비밀번호는 필수입니다.")));

        verify(authService, never()).login(any());
    }

    @Test
    @DisplayName("로그인 요청의 비밀번호가 공백이면 400 검증 실패 응답을 반환한다")
    void login_blankPassword_returnsBadRequest() throws Exception {
        // Given
        SignInRequest request = new SignInRequest("user@example.com", "   ");

        // When & Then
        mockMvc.perform(post("/api/auth/sign-in")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", request.username())
                        .param("password", request.password()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.details.password")
                        .value(org.hamcrest.Matchers.hasItem("비밀번호는 필수입니다.")));

        verify(authService, never()).login(any());
    }

    @Test
    @DisplayName("인증 실패 예외가 발생하면 401 인증 실패 응답을 반환한다")
    void login_invalidCredentials_returnsUnauthorized() throws Exception {
        // Given
        SignInRequest request = new SignInRequest("user@example.com", "wrong-password");
        given(authService.login(any(SignInRequest.class)))
                .willThrow(new InvalidCredentialsException("비밀번호가 일치하지 않습니다."));

        // When & Then
        mockMvc.perform(post("/api/auth/sign-in")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", request.username())
                .param("password", request.password()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.exceptionType").value("InvalidCredentialsException"))
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다."))
                .andExpect(jsonPath("$.details").doesNotExist());

        verify(authService).login(request);
    }

    @Test
    @DisplayName("인증 헤더 없이 로그아웃을 요청하면 refresh token cookie 삭제 응답을 반환한다")
    void logout_missingAuthorizationHeader_deletesRefreshTokenCookie() throws Exception {
        // Given: Authorization 헤더 없음
        ResponseCookie deleteCookie = ResponseCookie.from("REFRESH_TOKEN", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(0)
                .build();
        given(cookieManager.deleteCookie()).willReturn(deleteCookie);

        // When & Then
        mockMvc.perform(post("/api/auth/sign-out")
                        .with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("REFRESH_TOKEN="),
                                org.hamcrest.Matchers.containsString("Path=/api/auth"),
                                org.hamcrest.Matchers.containsString("Max-Age=0"),
                                org.hamcrest.Matchers.containsString("HttpOnly"),
                                org.hamcrest.Matchers.containsString("SameSite=Lax")
                        )));

        verify(authService).logout(null);
        verify(cookieManager).deleteCookie();
    }

    @Test
    @DisplayName("인증 헤더가 있으면 로그아웃에 성공한다")
    void logout_authenticatedUser_success() throws Exception {
        // Given
        String accessToken = "valid-access-token";
        String refreshToken = "refresh-token";
        mockAccessToken(accessToken);
        ResponseCookie deleteCookie = ResponseCookie.from("REFRESH_TOKEN", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(0)
                .build();
        given(cookieManager.deleteCookie()).willReturn(deleteCookie);

        // When & Then
        mockMvc.perform(post("/api/auth/sign-out")
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken)
                        .cookie(new jakarta.servlet.http.Cookie("REFRESH_TOKEN", refreshToken)))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("Max-Age=0"),
                                org.hamcrest.Matchers.containsString("SameSite=Lax")
                        )));

        verify(authService).logout(refreshToken);
        verify(cookieManager).deleteCookie();
    }

    @Test
    @DisplayName("인증된 사용자가 CSRF 토큰 없이 로그아웃 요청하면 403을 반환한다")
    void signOut_authenticatedWithoutCsrf_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/auth/sign-out")
                        .with(user("test@example.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("로그아웃 서비스 예외가 발생하면 500 서버 오류 응답을 반환한다")
    void logout_serviceException_returnsInternalServerError() throws Exception {
        // Given
        String accessToken = "valid-access-token";
        String refreshToken = "refresh-token";
        mockAccessToken(accessToken);
        org.mockito.BDDMockito.willThrow(new IllegalStateException("logout failed"))
                .given(authService)
                .logout(refreshToken);

        // When & Then
        mockMvc.perform(post("/api/auth/sign-out")
                .with(csrf())
                .header("Authorization", "Bearer " + accessToken)
                .cookie(new jakarta.servlet.http.Cookie("REFRESH_TOKEN", refreshToken)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.exceptionType").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 내부 에러가 발생했습니다."))
                .andExpect(jsonPath("$.details").doesNotExist());

        verify(authService).logout(refreshToken);
    }

    @Test
    @DisplayName("잘못된 JWT로 인증이 실패하면 AuthenticationEntryPoint 응답을 반환한다")
    void authenticatedRequest_invalidJwt_returnsEntryPointResponse() throws Exception {
        // Given
        String accessToken = "invalid-access-token";
        given(jwtTokenizer.getAccessClaims(accessToken))
                .willThrow(new MalformedJwtException("Invalid access token"));

        // When & Then
        mockMvc.perform(post("/api/follows")
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.exceptionType").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.exceptionName").doesNotExist())
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

    @Test
    @DisplayName("CSRF 토큰 발급 요청에 성공하면 XSRF-TOKEN 쿠키를 반환한다")
    void csrfToken_success() throws Exception {
        // Given: CSRF 토큰 발급 엔드포인트

        // When & Then
        mockMvc.perform(get("/api/auth/csrf-token"))
                .andExpect(status().isNoContent())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("XSRF-TOKEN="))));
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰이면 인증 실패 응답을 반환한다")
    void refresh_invalidRefreshToken_returnsUnauthorized() throws Exception {
        // Given
        ResponseCookie deleteCookie = ResponseCookie.from("REFRESH_TOKEN", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(0)
                .build();
        given(authService.refresh("refresh-token"))
                .willThrow(new RefreshTokenInvalidException("Invalid refresh token"));
        given(cookieManager.deleteCookie()).willReturn(deleteCookie);

        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("REFRESH_TOKEN", "refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("REFRESH_TOKEN="),
                                org.hamcrest.Matchers.containsString("Max-Age=0"),
                                org.hamcrest.Matchers.containsString("SameSite=Lax")
                        )))
                .andExpect(jsonPath("$.exceptionType").value("RefreshTokenInvalidException"))
                .andExpect(jsonPath("$.message").value("Invalid refresh token"))
                .andExpect(jsonPath("$.details").doesNotExist());

        verify(authService).refresh("refresh-token");
        verify(cookieManager).deleteCookie();
    }

    @Test
    @DisplayName("토큰 재발급 요청에 성공하면 refresh token cookie를 반환한다")
    void refresh_success() throws Exception {
        // Given
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        JwtResponse response = new JwtResponse(
                new UserResponse(
                        userId,
                        Instant.parse("2026-06-24T00:00:00Z"),
                        "user@example.com",
                        "사용자",
                        null,
                        "USER",
                        false
                ),
                "new-access-token"
        );
        AuthPayload authPayload = new AuthPayload(response, "new-refresh-token");
        ResponseCookie refreshTokenCookie = ResponseCookie.from("REFRESH_TOKEN", "new-refresh-token")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(420 * 60)
                .build();
        given(authService.refresh("refresh-token")).willReturn(authPayload);
        given(cookieManager.createCookie("new-refresh-token")).willReturn(refreshTokenCookie);

        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("REFRESH_TOKEN", "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("REFRESH_TOKEN=new-refresh-token"),
                                org.hamcrest.Matchers.containsString("Max-Age=25200"),
                                org.hamcrest.Matchers.containsString("SameSite=Lax")
                        )))
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist());

        verify(authService).refresh("refresh-token");
        verify(cookieManager).createCookie("new-refresh-token");
    }

    private void mockAccessToken(String accessToken) {
        @SuppressWarnings("unchecked")
        Jws<Claims> claimsJws = org.mockito.Mockito.mock(Jws.class);
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        UserResponse userResponse = new UserResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                Instant.parse("2026-06-24T00:00:00Z"),
                "user@example.com",
                "사용자",
                null,
                "USER",
                false
        );
        MoplUserDetails userDetails = new MoplUserDetails(userResponse, "encoded-password");
        User user = User.create(userResponse.email(), "encoded-password", userResponse.name());
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", userResponse.id());

        given(jwtTokenizer.getAccessClaims(accessToken)).willReturn(claimsJws);
        given(claimsJws.getBody()).willReturn(claims);
        given(claims.getSubject()).willReturn(userResponse.id().toString());
        given(claims.get("email", String.class)).willReturn("user@example.com");
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(userDetailsService.loadUserByUsername("user@example.com")).willReturn(userDetails);
    }
}
