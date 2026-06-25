package com.codeit.team5.mopl.auth.utils;

import com.codeit.team5.mopl.global.dto.ErrorResponse;
import com.codeit.team5.mopl.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;

public class ErrorResponder {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void sendErrorResponse(
            HttpServletResponse response,
            ErrorCode errorCode
    ) throws IOException {

        ErrorResponse errorResponse = ErrorResponse.of(errorCode);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(errorCode.getHttpStatus().value());

        response.getWriter().write(
                objectMapper.writeValueAsString(errorResponse)
        );
    }
}
