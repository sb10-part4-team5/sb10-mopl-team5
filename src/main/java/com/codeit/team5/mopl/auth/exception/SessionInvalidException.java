package com.codeit.team5.mopl.auth.exception;

import org.springframework.http.HttpStatus;

public class SessionInvalidException extends AuthException {

    public SessionInvalidException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
