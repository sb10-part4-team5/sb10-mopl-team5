package com.codeit.team5.mopl.auth.security.handler.signout;

import com.codeit.team5.mopl.auth.service.AuthSessionService;
import com.codeit.team5.mopl.auth.support.RefreshTokenCookieManager;
import com.codeit.team5.mopl.auth.exception.RefreshTokenInvalidException;
import com.codeit.team5.mopl.auth.jwt.JwtTokenizer;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignOutHandler implements LogoutHandler {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "REFRESH_TOKEN";

    private final JwtTokenizer jwtTokenizer;
    private final AuthSessionService authSessionService;
    private final RefreshTokenCookieManager cookieManager;

    @Override
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) {
        Cookie refreshTokenCookie =
                WebUtils.getCookie(request, REFRESH_TOKEN_COOKIE_NAME);

        if (refreshTokenCookie != null) {
            signOut(refreshTokenCookie.getValue());
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
            authSessionService.invalidateUserSessions(userId);
            log.info("Logout success");
        } catch (RefreshTokenInvalidException e) {
            log.info("Logout requested with invalid or expired refresh token");
        }
        catch (RuntimeException e) {
            log.error("Logout failed while deleting refresh token", e);
        }
    }
}
