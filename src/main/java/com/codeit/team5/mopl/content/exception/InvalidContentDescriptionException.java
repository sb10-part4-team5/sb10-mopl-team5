package com.codeit.team5.mopl.content.exception;

import org.springframework.http.HttpStatus;

public class InvalidContentDescriptionException extends ContentException {

    public InvalidContentDescriptionException() {
        super(HttpStatus.BAD_REQUEST, "설명은 필수입니다.");
    }
}
