package com.codeit.team5.mopl.tag.exception;

import com.codeit.team5.mopl.global.exception.ErrorCode;

public class InvalidTagNameException extends TagException {

    public InvalidTagNameException(String detailMessage) {
        super(ErrorCode.INVALID_TAG_NAME, detailMessage);
    }
}
