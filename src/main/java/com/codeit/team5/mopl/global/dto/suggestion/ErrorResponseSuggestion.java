package com.codeit.team5.mopl.global.dto.suggestion;

import com.codeit.team5.mopl.global.exception.BusinessException;
import com.codeit.team5.mopl.global.exception.util.ViolationExceptionUtils;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

public record ErrorResponseSuggestion(String exceptionType, String message, Object details) {

    public static ErrorResponseSuggestion from(BusinessException ex) {
        return new ErrorResponseSuggestion(ex.getExceptionType(), ex.getMessage(), null);
    }

    public static ErrorResponseSuggestion from(MethodArgumentNotValidException ex) {
        Map<String, List<String>> details = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(
                        FieldError::getField,
                        Collectors.mapping(
                                error -> error.getDefaultMessage() == null ? "유효하지 않은 입력값입니다."
                                        : error.getDefaultMessage(),
                                Collectors.toList()
                        )
                ));
        return new ErrorResponseSuggestion("INVALID_INPUT", "잘못된 입력값입니다.", details);
    }

    public static ErrorResponseSuggestion from(ConstraintViolationException ex) {
        boolean isFromController = ViolationExceptionUtils.isFromController(ex);
        if (isFromController) {
            Map<String, List<String>> details = ex.getConstraintViolations().stream()
                    .collect(Collectors.groupingBy(
                            v -> {
                                String path = v.getPropertyPath().toString();
                                return path.substring(path.lastIndexOf('.') + 1);
                            },
                            Collectors.mapping(ConstraintViolation::getMessage, Collectors.toList())
                    ));
            return new ErrorResponseSuggestion("INVALID_INPUT", "잘못된 입력값입니다.", details);
        }
        return new ErrorResponseSuggestion("INTERNAL_SERVER_ERROR",
                "서버 내부 데이터 처리 중 오류가 발생했습니다.", null);
    }

    public static ErrorResponseSuggestion from(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof InvalidFormatException invalidFormatException) {
            List<JsonMappingException.Reference> path = invalidFormatException.getPath();

            String fieldName = "request";

            // 배열 인덱스 등 fieldName이 null인 경우를 고려해 뒤에서부터 실제 필드명을 찾는다.
            for (int i = path.size() - 1; i >= 0; i--) {
                String candidate = path.get(i).getFieldName();
                if (candidate != null) {
                    fieldName = candidate;
                    break;
                }
            }

            Object invalidValue = invalidFormatException.getValue();

            return new ErrorResponseSuggestion(
                    "INVALID_INPUT",
                    "잘못된 입력값입니다.",
                    Map.of(
                            fieldName,
                            List.of("허용되지 않는 값입니다: " + invalidValue)
                    )
            );
        }

        return new ErrorResponseSuggestion(
                "INVALID_INPUT",
                "요청 본문을 읽을 수 없습니다.",
                null
        );
    }

    public static ErrorResponseSuggestion from(MissingServletRequestParameterException ex) {
        Map<String, List<String>> details = Map.of(
                ex.getParameterName(),
                List.of("필수 파라미터 '" + ex.getParameterName() + "'이(가) 누락되었습니다.")
        );
        return new ErrorResponseSuggestion("INVALID_INPUT", "잘못된 입력값입니다.", details);
    }
}
