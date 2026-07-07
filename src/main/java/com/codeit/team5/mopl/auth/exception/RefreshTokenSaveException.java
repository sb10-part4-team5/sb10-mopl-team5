package com.codeit.team5.mopl.auth.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class RefreshTokenSaveException extends AuthException {

    public RefreshTokenSaveException(Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "refreshToken 저장에 실패했습니다.");
        initCause(cause);
    }
}
