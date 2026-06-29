package com.codeit.team5.mopl.auth.exception;

import org.springframework.http.HttpStatus;

public class RefreshTokenNotFoundException extends AuthException {

    public RefreshTokenNotFoundException() {
        super(HttpStatus.UNAUTHORIZED, "refreshToken이 존재하지 않습니다.");
    }
}
