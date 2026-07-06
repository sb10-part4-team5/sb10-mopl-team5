package com.codeit.team5.mopl.content.exception;

import org.springframework.http.HttpStatus;

public class TooManyTagsException extends ContentException {

    public TooManyTagsException() {
        super(HttpStatus.BAD_REQUEST, "태그는 최대 10개까지 등록할 수 있습니다.");
    }
}
