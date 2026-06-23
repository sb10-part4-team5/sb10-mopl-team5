package com.codeit.team5.mopl.global.dto.suggestion;

import com.codeit.team5.mopl.global.exception.suggestion.BusinessExceptionSuggestion;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestController;

public record ErrorResponseSuggestion(String exceptionType, String message, Object details) {

    public static ErrorResponseSuggestion from(BusinessExceptionSuggestion ex) {
        return new ErrorResponseSuggestion(ex.getExceptionType(), ex.getMessage(), null);
    }

    public static ErrorResponseSuggestion from(MethodArgumentNotValidException ex) {
        Map<String, List<String>> details = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(
                        FieldError::getField,
                        Collectors.mapping(
                                error -> error.getDefaultMessage() == null ? ""
                                        : error.getDefaultMessage(),
                                Collectors.toList()
                        )
                ));
        return new ErrorResponseSuggestion("INVALID_INPUT", "잘못된 입력값입니다.", details);
    }

    public static ErrorResponseSuggestion from(ConstraintViolationException ex) {
        boolean isFromController = ex.getConstraintViolations().stream()
                .anyMatch(v -> v.getRootBeanClass().getSimpleName().endsWith("Controller")
                        || v.getRootBeanClass().isAnnotationPresent(RestController.class));
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
}
