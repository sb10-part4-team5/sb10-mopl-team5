package com.codeit.team5.mopl.auth.exception;

import org.springframework.http.HttpStatus;

public abstract class RefreshTokenException extends AuthException {

    protected RefreshTokenException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
