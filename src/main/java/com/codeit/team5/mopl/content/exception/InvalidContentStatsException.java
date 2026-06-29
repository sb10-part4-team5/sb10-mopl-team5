package com.codeit.team5.mopl.content.exception;

import org.springframework.http.HttpStatus;

public class InvalidContentStatsException extends ContentException {

    public InvalidContentStatsException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
