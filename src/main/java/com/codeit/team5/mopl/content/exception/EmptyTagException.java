package com.codeit.team5.mopl.content.exception;

import org.springframework.http.HttpStatus;

public class EmptyTagException extends ContentException {

    public EmptyTagException() {
        super(HttpStatus.BAD_REQUEST, "정규화 후 유효한 태그가 없습니다.");
    }
}
