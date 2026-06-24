package com.codeit.team5.mopl.tag.exception;

import org.springframework.http.HttpStatus;

public class InvalidTagNameException extends TagException {

    public InvalidTagNameException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
