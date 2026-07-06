package com.codeit.team5.mopl.auth.security.handler.signin;

import com.codeit.team5.mopl.auth.jwt.JwtProperties;
import com.codeit.team5.mopl.auth.jwt.JwtTokenizer;
import com.codeit.team5.mopl.auth.security.details.MoplPrincipal;
import com.codeit.team5.mopl.auth.service.RefreshTokenStore;
import com.codeit.team5.mopl.auth.support.RefreshTokenCookieManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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
    }
}
