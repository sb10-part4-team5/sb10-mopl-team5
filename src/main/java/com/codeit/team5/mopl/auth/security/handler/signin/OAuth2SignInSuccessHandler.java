package com.codeit.team5.mopl.auth.security.handler.signin;

import com.codeit.team5.mopl.auth.exception.RefreshTokenSaveException;
import com.codeit.team5.mopl.auth.jwt.JwtProperties;
import com.codeit.team5.mopl.auth.jwt.JwtTokenizer;
import com.codeit.team5.mopl.auth.security.details.MoplPrincipal;
import com.codeit.team5.mopl.auth.service.RefreshTokenStore;
import com.codeit.team5.mopl.auth.support.RefreshTokenCookieManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SignInSuccessHandler implements AuthenticationSuccessHandler {

    private final RefreshTokenCookieManager cookieManager;
    private final RefreshTokenStore refreshTokenStore;
    private final JwtTokenizer jwtTokenizer;
    private final JwtProperties jwtProperties;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        MoplPrincipal principal = (MoplPrincipal) authentication.getPrincipal();

        try {
            String refreshToken = jwtTokenizer.generateRefreshToken(principal.getId().toString());

            refreshTokenStore.save(
                    principal.getId(),
                    refreshToken,
                    Instant.now().plus(
                            jwtProperties.refreshTokenExpirationMinutes(),
                            ChronoUnit.MINUTES
                    )
            );

            ResponseCookie responseCookie = cookieManager.createCookie(refreshToken);
            response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());

            log.info("OAuth2 login success. userId={}", principal.getId());

            response.sendRedirect("/");

        } catch (RefreshTokenSaveException e) {
            log.error("OAuth2 login refresh token save failed. userId={}", principal.getId(), e);

            response.sendRedirect(
                    "/#/sign-in?error=oauth_failed&error_message="
                            + URLEncoder.encode("OAuth 로그인 처리 중 오류가 발생했습니다.", StandardCharsets.UTF_8)
            );
        }
    }
}
