package com.codeit.team5.mopl.auth.security.handler.signout;

import com.codeit.team5.mopl.auth.support.RefreshTokenCookieManager;
import com.codeit.team5.mopl.auth.exception.RefreshTokenInvalidException;
import com.codeit.team5.mopl.auth.jwt.JwtTokenizer;
import com.codeit.team5.mopl.auth.service.RefreshTokenStore;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignOutHandler implements LogoutHandler {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "REFRESH_TOKEN";

    private final JwtTokenizer jwtTokenizer;
    private final RefreshTokenStore refreshTokenStore;
    private final RefreshTokenCookieManager cookieManager;

    @Override
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            Arrays.stream(cookies)
                    .filter(cookie -> REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName()))
                    .findFirst()
                    .ifPresent(cookie -> signOut(cookie.getValue()));
        }

        ResponseCookie deleteCookie = cookieManager.deleteCookie();
        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
    }

    private void signOut(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            log.info("Logout requested without refresh token");
            return;
        }

        try {
            UUID userId = jwtTokenizer.getRefreshUserId(refreshToken);
            refreshTokenStore.deleteByUserId(userId);
            log.info("Logout success");
        } catch (RefreshTokenInvalidException e) {
            log.info("Logout requested with invalid or expired refresh token");
        }
    }
}
