package com.codeit.team5.mopl.content.exception;

import org.springframework.http.HttpStatus;

public class InvalidContentSourceException extends ContentException {

    public InvalidContentSourceException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
