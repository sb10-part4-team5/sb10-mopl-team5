package com.codeit.team5.mopl.auth.security.handler.signin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OAuth2SignInFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {

        log.warn("OAuth2 login failed", exception);

        String errorMessage = URLEncoder.encode("소셜 로그인으로 로그인에 실패하였습니다.", StandardCharsets.UTF_8);
        response.sendRedirect("/#/sign-in?error=oauth_failed&error_message=" + errorMessage);
    }
}
