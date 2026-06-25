package com.codeit.team5.mopl.auth.exception;

import org.springframework.http.HttpStatus;

public class JwtInvalidException extends AuthException {

    public JwtInvalidException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
