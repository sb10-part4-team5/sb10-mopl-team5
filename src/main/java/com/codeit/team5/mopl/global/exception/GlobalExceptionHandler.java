package com.codeit.team5.mopl.global.exception;

import com.codeit.team5.mopl.auth.support.RefreshTokenCookieManager;
import com.codeit.team5.mopl.auth.exception.AuthException;
import com.codeit.team5.mopl.auth.exception.RefreshTokenExpiredException;
import com.codeit.team5.mopl.auth.exception.RefreshTokenInvalidException;
import com.codeit.team5.mopl.auth.exception.RefreshTokenNotFoundException;
import com.codeit.team5.mopl.global.dto.ErrorResponse;
import com.codeit.team5.mopl.global.exception.util.ViolationExceptionUtils;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final RefreshTokenCookieManager refreshTokenCookieManager;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.warn(e.toString());
        return ResponseEntity
                .status(e.getStatus())
                .body(ErrorResponse.from(e));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
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
                .body(ErrorResponse.from(e));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
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
                .body(ErrorResponse.from(e));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e
    ) {
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.from(e));
    }

    // 클라이언트가 SSE 연결을 끊을 때(브라우저 탭 닫기, 새로고침 등) 응답 스트림에 쓰다가 발생하는
    // broken pipe성 IOException. 클라이언트가 이미 떠난 상태라 응답을 보낼 수 없고, 버그도 아니므로
    // ERROR로 로그를 남기지 않고 조용히 무시한다.
    @ExceptionHandler(IOException.class)
    public void handleIOException(IOException e) {
        log.debug("클라이언트 연결 종료로 응답 전송 실패: {}", e.getMessage());
    }

    // 배치 작업 스레드 풀의 큐까지 가득 찬 상태에서 새 수집 요청이 들어오면 AbortPolicy가
    // TaskRejectedException을 던진다. 일시적인 과부하이므로 503으로 매핑해 재시도를 유도한다.
    @ExceptionHandler(TaskRejectedException.class)
    public ResponseEntity<ErrorResponse> handleTaskRejectedException(TaskRejectedException e) {
        log.warn("배치 작업 큐 포화로 요청 거부: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("SERVICE_UNAVAILABLE",
                        "현재 수집 작업이 많아 요청을 처리할 수 없습니다. 잠시 후 다시 시도해주세요.", null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected exception", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_SERVER_ERROR",
                        "서버 내부 에러가 발생했습니다.", null));
    }

    @ExceptionHandler({
            RefreshTokenInvalidException.class,
            RefreshTokenNotFoundException.class,
            RefreshTokenExpiredException.class
    })
    public ResponseEntity<ErrorResponse> handleRefreshTokenException(AuthException e) {
        ResponseCookie deleteCookie = refreshTokenCookieManager.deleteCookie();

        return ResponseEntity.status(e.getStatus())
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(ErrorResponse.from(e));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
        MissingServletRequestParameterException e
    ){
        log.warn(e.toString());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.from(e));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
        MethodArgumentTypeMismatchException e
    ) {
        log.warn(e.toString());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.from(e));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException e
    ) {
        log.warn("데이터 무결성 제약조건 위반: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.from(e));
    }
}
