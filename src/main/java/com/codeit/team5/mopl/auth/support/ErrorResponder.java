package com.codeit.team5.mopl.auth.support;

import com.codeit.team5.mopl.global.dto.ErrorResponse;
import com.codeit.team5.mopl.global.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

public class ErrorResponder {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void sendErrorResponse(
            HttpServletResponse response,
            BusinessException exception
    ) throws IOException {

        ErrorResponse errorResponse = ErrorResponse.from(exception);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(exception.getStatus().value());

        response.getWriter().write(
                objectMapper.writeValueAsString(errorResponse)
        );
    }

    public static void sendErrorResponse(
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED.value(),
                new ErrorResponse(
                        "UNAUTHORIZED",
                        "인증이 필요합니다.",
                        null
                ));
    }

    public static void sendErrorResponse(
            HttpServletResponse response,
            AccessDeniedException exception
    ) throws IOException {
        write(response, HttpStatus.FORBIDDEN.value(),
                new ErrorResponse(
                        "FORBIDDEN",
                        "접근 권한이 없습니다.",
                        null
                ));
    }

    private static void write(
            HttpServletResponse response,
            int status,
            ErrorResponse errorResponse
    ) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(status);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
