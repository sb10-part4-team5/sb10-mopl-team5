package com.codeit.team5.mopl.auth.handler.signin;

import com.codeit.team5.mopl.global.dto.suggestion.ErrorResponseSuggestion;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SignInFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        int status = exception instanceof LockedException
                ? HttpServletResponse.SC_FORBIDDEN
                : HttpServletResponse.SC_UNAUTHORIZED;

        ErrorResponseSuggestion body = new ErrorResponseSuggestion(
                exception instanceof LockedException ? "ACCOUNT_LOCKED" : "INVALID_CREDENTIALS",
                exception instanceof LockedException ? "잠긴 계정입니다." : "이메일 또는 비밀번호가 올바르지 않습니다.",
                exception instanceof LockedException
                        ? null
                        : Map.of("loginFailed", List.of("Invalid credentials"))
        );

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(response.getWriter(), body);
    }
}
