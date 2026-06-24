package com.codeit.team5.mopl.content.exception;

import com.codeit.team5.mopl.global.exception.ErrorCode;

public class InvalidContentSourceException extends ContentException {

    public InvalidContentSourceException(String detailMessage) {
        super(ErrorCode.INVALID_CONTENT_SOURCE, detailMessage);
    }
}
