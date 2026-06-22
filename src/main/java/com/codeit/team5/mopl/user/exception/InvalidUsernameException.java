package com.codeit.team5.mopl.user.exception;

import com.codeit.team5.mopl.global.exception.ErrorCode;

public class InvalidUsernameException extends UserException {

    public InvalidUsernameException() {
        super(ErrorCode.INVALID_USERNAME);
    }
}
