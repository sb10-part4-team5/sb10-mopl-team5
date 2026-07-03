package com.codeit.team5.mopl.auth.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class TemporaryPasswordNotFoundException extends AuthException {

    public TemporaryPasswordNotFoundException(UUID userId) {
        super(HttpStatus.NOT_FOUND, "해당 userId로 발급된 임시비밀번호를 찾을 수 없습니다.", Map.of("userId", userId));
    }
}
