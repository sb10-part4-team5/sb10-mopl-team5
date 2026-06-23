package com.codeit.team5.mopl.user.exception;

import com.codeit.team5.mopl.global.exception.ErrorCode;

public class DuplicatedEmailException extends UserException {

    public DuplicatedEmailException(ErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
