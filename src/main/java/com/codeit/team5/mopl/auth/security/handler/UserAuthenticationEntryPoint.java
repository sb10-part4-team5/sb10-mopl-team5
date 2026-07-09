package com.codeit.team5.mopl.auth.security.handler;

import com.codeit.team5.mopl.auth.jwt.JwtAuthenticationFilter;
import com.codeit.team5.mopl.auth.support.ErrorResponder;
import com.codeit.team5.mopl.global.exception.BusinessException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        Exception exception = (Exception) request.getAttribute(
                JwtAuthenticationFilter.AUTH_EXCEPTION_ATTRIBUTE
        );

        if (exception instanceof BusinessException businessException) {
            log.warn("Unauthorized access: {}", businessException.toString());
            ErrorResponder.sendErrorResponse(response, businessException);
            return;
        }

        if (exception instanceof LockedException lockedException) {
            log.warn("Locked account access: {}", lockedException.toString());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            ErrorResponder.sendErrorResponse(response, lockedException);
            return;
        }

        if (exception != null) {
            log.warn("Unauthorized access: {}", exception.toString());
        } else {
            log.warn("Unauthorized access", authException);
        }
        ErrorResponder.sendErrorResponse(response, authException);
    }
}
