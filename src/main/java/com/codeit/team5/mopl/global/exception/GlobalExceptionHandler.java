package com.codeit.team5.mopl.global.exception;

import com.codeit.team5.mopl.auth.cookie.RefreshTokenCookieManager;
import com.codeit.team5.mopl.auth.exception.AuthException;
import com.codeit.team5.mopl.auth.exception.RefreshTokenExpiredException;
import com.codeit.team5.mopl.auth.exception.RefreshTokenInvalidException;
import com.codeit.team5.mopl.auth.exception.RefreshTokenNotFoundException;
import com.codeit.team5.mopl.global.dto.suggestion.ErrorResponseSuggestion;
import com.codeit.team5.mopl.global.exception.util.ViolationExceptionUtils;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final RefreshTokenCookieManager refreshTokenCookieManager;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseSuggestion> handleBusinessException(BusinessException e) {
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseSuggestion> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e
    ) {
        return ResponseEntity
                .badRequest()
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

    @ExceptionHandler({
            RefreshTokenInvalidException.class,
            RefreshTokenNotFoundException.class,
            RefreshTokenExpiredException.class
    })
    public ResponseEntity<ErrorResponseSuggestion> handleRefreshTokenException(AuthException e) {
        ResponseCookie deleteCookie = refreshTokenCookieManager.deleteCookie();

        return ResponseEntity.status(e.getStatus())
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(ErrorResponseSuggestion.from(e));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseSuggestion> handleMissingServletRequestParameterException(
        MissingServletRequestParameterException e
    ){
        log.warn(e.toString());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponseSuggestion.from(e));
    }

}
