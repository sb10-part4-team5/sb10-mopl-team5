package com.codeit.team5.mopl.auth.exception;

import org.springframework.http.HttpStatus;

public class AccountLockedException extends AuthException {

    public AccountLockedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
