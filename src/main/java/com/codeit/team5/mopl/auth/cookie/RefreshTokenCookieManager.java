package com.codeit.team5.mopl.auth.cookie;

import com.codeit.team5.mopl.auth.jwt.JwtProperties;
import java.time.Duration;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieManager {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "REFRESH_TOKEN";
    private static final String REFRESH_TOKEN_COOKIE_PATH = "/api/auth";

    private final JwtProperties jwtProperties;
    private final AuthCookieProperties authCookieProperties;

    public ResponseCookie createCookie(String refreshToken) {
        Objects.requireNonNull(refreshToken, "refreshToken must not be null");

        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(authCookieProperties.secure())
                .sameSite(authCookieProperties.sameSite())
                .path(REFRESH_TOKEN_COOKIE_PATH)
                .maxAge(Duration.ofMinutes(jwtProperties.refreshTokenExpirationMinutes()))
                .build();
    }

    public ResponseCookie deleteCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(authCookieProperties.secure())
                .sameSite(authCookieProperties.sameSite())
                .path(REFRESH_TOKEN_COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }
}
