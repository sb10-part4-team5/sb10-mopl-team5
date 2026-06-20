package com.codeit.team5.mopl.global.dto;

import com.codeit.team5.mopl.global.exception.ErrorCode;
import java.util.Map;

public record ErrorResponse(
    String exceptionName,
    String message,
    Map<String, String> details
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(
            errorCode.name(),
            errorCode.getMessage(),
            Map.of()
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(
            errorCode.name(),
            message,
            Map.of()
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, Map<String, String> details) {
        return new ErrorResponse(
            errorCode.name(),
            errorCode.getMessage(),
            details
        );
    }
}
