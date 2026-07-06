package com.codeit.team5.mopl.tag.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class TagException extends BusinessException {

    public TagException(HttpStatus status, String message) {
        super(status, message);
    }
}
