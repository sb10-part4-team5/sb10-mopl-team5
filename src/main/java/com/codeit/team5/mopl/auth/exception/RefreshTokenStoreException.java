package com.codeit.team5.mopl.auth.exception;

import org.springframework.http.HttpStatus;

public class RefreshTokenStoreException extends AuthException {

    private static final String MESSAGE =
            "리프레시 토큰 저장소 처리 중 오류가 발생했습니다.";

    public RefreshTokenStoreException(Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, MESSAGE);
        initCause(cause);
    }

    public RefreshTokenStoreException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
