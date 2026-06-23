package com.codeit.team5.mopl.user.exception;

import com.codeit.team5.mopl.global.exception.ErrorCode;

public class InvalidPasswordException extends UserException {

    public InvalidPasswordException() {
        super(ErrorCode.INVALID_PASSWORD);
    }
}
