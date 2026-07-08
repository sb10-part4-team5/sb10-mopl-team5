package com.codeit.team5.mopl.auth.security.handler.signin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.auth.jwt.JwtProperties;
import com.codeit.team5.mopl.auth.jwt.JwtTokenizer;
import com.codeit.team5.mopl.auth.security.details.AuthUser;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.auth.service.AuthSessionService;
import com.codeit.team5.mopl.auth.service.RefreshTokenStore;
import com.codeit.team5.mopl.auth.support.RefreshTokenCookieManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class OAuth2SignInSuccessHandlerTest {

    @Mock
    private AuthSessionService authSessionService;

    @Mock
    private RefreshTokenCookieManager cookieManager;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @Mock
    private JwtTokenizer jwtTokenizer;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private OAuth2SignInSuccessHandler handler;

    @Test
    @DisplayName("OAuth 로그인 성공 시 로그인 세션과 리프레시 토큰을 저장하고 홈으로 리다이렉트한다")
    void onAuthenticationSuccess_success() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        String refreshToken = "oauth-refresh-token";
        MoplUserDetails principal = new MoplUserDetails(
                new AuthUser(userId, "oauth@example.com", "USER", false),
                ""
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        ResponseCookie refreshTokenCookie = ResponseCookie.from("REFRESH_TOKEN", refreshToken)
                .httpOnly(true)
                .path("/api/auth")
                .maxAge(420 * 60)
                .build();

        when(jwtProperties.refreshTokenExpirationMinutes()).thenReturn(420L);
        when(authSessionService.replaceUserSession(eq(userId), any(Instant.class))).thenReturn(sessionId);
        when(jwtTokenizer.generateRefreshToken(userId.toString())).thenReturn(refreshToken);
        when(cookieManager.createCookie(refreshToken)).thenReturn(refreshTokenCookie);

        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        // Then
        ArgumentCaptor<Instant> expiresAtCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(authSessionService).replaceUserSession(eq(userId), expiresAtCaptor.capture());
        verify(jwtTokenizer).generateRefreshToken(userId.toString());
        verify(refreshTokenStore, atLeastOnce()).save(eq(userId), eq(refreshToken), eq(expiresAtCaptor.getValue()));
        verify(cookieManager).createCookie(refreshToken);

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).contains("REFRESH_TOKEN=oauth-refresh-token");
        assertThat(response.getRedirectedUrl()).isEqualTo("/");
    }
}
