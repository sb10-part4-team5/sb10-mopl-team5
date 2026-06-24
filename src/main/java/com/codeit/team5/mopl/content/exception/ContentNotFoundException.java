package com.codeit.team5.mopl.content.exception;

import com.codeit.team5.mopl.global.exception.ErrorCode;

public class ContentNotFoundException extends ContentException {

    public ContentNotFoundException() {
        super(ErrorCode.CONTENT_NOT_FOUND);
    }
}
