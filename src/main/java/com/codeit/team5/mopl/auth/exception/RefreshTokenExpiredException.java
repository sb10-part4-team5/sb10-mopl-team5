package com.codeit.team5.mopl.auth.exception;

import org.springframework.http.HttpStatus;

public class RefreshTokenExpiredException extends AuthException {

    public RefreshTokenExpiredException() {
        super(HttpStatus.UNAUTHORIZED, "만료된 refreshToken 입니다.");
    }
}
