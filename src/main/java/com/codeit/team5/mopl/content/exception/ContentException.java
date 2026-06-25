package com.codeit.team5.mopl.content.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class ContentException extends BusinessException {

    public ContentException(HttpStatus status, String message) {
        super(status, message);
    }
}
