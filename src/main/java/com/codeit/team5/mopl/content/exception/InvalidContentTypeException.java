package com.codeit.team5.mopl.content.exception;

import org.springframework.http.HttpStatus;

public class InvalidContentTypeException extends ContentException {

    public InvalidContentTypeException(String value) {
        super(HttpStatus.BAD_REQUEST, "유효하지 않은 콘텐츠 타입: " + value);
    }
}
