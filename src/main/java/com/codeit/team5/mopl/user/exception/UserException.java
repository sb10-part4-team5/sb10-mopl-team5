package com.codeit.team5.mopl.user.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import com.codeit.team5.mopl.global.exception.ErrorCode;

public class UserException extends BusinessException {

    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }

    public UserException(ErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
