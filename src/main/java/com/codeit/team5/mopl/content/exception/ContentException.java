package com.codeit.team5.mopl.content.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import com.codeit.team5.mopl.global.exception.ErrorCode;

public class ContentException extends BusinessException {

    public ContentException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ContentException(ErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
