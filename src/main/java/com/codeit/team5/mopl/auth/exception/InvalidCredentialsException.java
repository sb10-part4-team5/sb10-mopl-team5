package com.codeit.team5.mopl.auth.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends AuthException {

    public InvalidCredentialsException(String message) {
        super(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.", Map.of("loginFailed", message));
    }

    public InvalidCredentialsException(UUID userId) {
        super(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.", Map.of("loginFailed", userId));
    }
}
