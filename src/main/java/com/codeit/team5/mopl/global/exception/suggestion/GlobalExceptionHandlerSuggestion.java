package com.codeit.team5.mopl.global.exception.suggestion;

import com.codeit.team5.mopl.global.dto.suggestion.ErrorResponseSuggestion;
import com.codeit.team5.mopl.global.exception.suggestion.util.ViolationExceptionUtils;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
// @RestControllerAdvice
public class GlobalExceptionHandlerSuggestion {

    @ExceptionHandler(BusinessExceptionSuggestion.class)
    public ResponseEntity<ErrorResponseSuggestion> handleBusinessException(BusinessExceptionSuggestion e) {
        log.warn(e.toString());
        return ResponseEntity
                .status(e.getStatus())
                .body(ErrorResponseSuggestion.from(e));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseSuggestion> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        String detailedErrorLog = e.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("필드 [%s] - 입력값: [%s], 원인: [%s]",
                        error.getField(),
                        error.getRejectedValue(),
                        error.getDefaultMessage()))
                .collect(Collectors.joining(" | "));
        log.warn("유효성 검사 실패 (MethodArgumentNotValidException) -> {}", detailedErrorLog);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseSuggestion.from(e));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseSuggestion> handleConstraintViolationException(
            ConstraintViolationException e) {
        String detailedErrorLog = e.getConstraintViolations().stream()
                .map(v -> String.format("경로 [%s] - 입력값: [%s], 원인: [%s]",
                        v.getPropertyPath(),
                        v.getInvalidValue(),
                        v.getMessage()))
                .collect(Collectors.joining(" | "));
        log.warn("제약조건 위반 (ConstraintViolationException) -> {}", detailedErrorLog);

        boolean isFromController = ViolationExceptionUtils.isFromController(e);
        HttpStatus status =
                isFromController ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(status)
                .body(ErrorResponseSuggestion.from(e));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseSuggestion> handleException(Exception e) {
        log.error("Unexpected exception", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseSuggestion("INTERNAL_SERVER_ERROR",
                        "서버 내부 에러가 발생했습니다.", null));
    }
}
