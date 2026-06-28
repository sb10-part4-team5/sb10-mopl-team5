package com.codeit.team5.mopl.content.exception;

import org.springframework.http.HttpStatus;

public class InvalidCursorException extends ContentException {

    public InvalidCursorException(String cursor) {
        super(HttpStatus.BAD_REQUEST, "커서 값이 유효하지 않습니다. cursor={" + cursor + "}");
    }
}
