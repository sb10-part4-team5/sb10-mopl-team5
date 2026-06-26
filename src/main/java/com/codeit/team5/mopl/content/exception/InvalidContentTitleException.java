package com.codeit.team5.mopl.content.exception;

import org.springframework.http.HttpStatus;

public class InvalidContentTitleException extends ContentException {

    public InvalidContentTitleException() {
        super(HttpStatus.BAD_REQUEST, "제목은 필수입니다.");
    }
}
