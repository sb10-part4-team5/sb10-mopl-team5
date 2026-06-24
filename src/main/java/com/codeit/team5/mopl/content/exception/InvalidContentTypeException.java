package com.codeit.team5.mopl.content.exception;

import com.codeit.team5.mopl.global.exception.ErrorCode;

public class InvalidContentTypeException extends ContentException {

    public InvalidContentTypeException(String value) {
        super(ErrorCode.INVALID_INPUT, "유효하지 않은 콘텐츠 타입: " + value);
    }
}
