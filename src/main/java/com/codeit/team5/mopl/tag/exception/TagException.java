package com.codeit.team5.mopl.tag.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import com.codeit.team5.mopl.global.exception.ErrorCode;

public class TagException extends BusinessException {

    public TagException(ErrorCode errorCode) {
        super(errorCode);
    }

    public TagException(ErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
