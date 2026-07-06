package com.codeit.team5.mopl.global.dto;

import com.codeit.team5.mopl.global.exception.ErrorCode;
import java.util.List;
import java.util.Map;

public record ErrorResponse(
    String exceptionName,
    String message,
    Map<String, List<String>> details
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

    public static ErrorResponse of(ErrorCode errorCode, Map<String, List<String>> details) {
        Map<String, List<String>> immutableDetails = details == null
                ? Map.of()
                : details.entrySet().stream()
                    .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                            e -> List.copyOf(e.getValue())
                ));

        return new ErrorResponse(
            errorCode.name(),
            errorCode.getMessage(),
            immutableDetails
        );
    }
}
