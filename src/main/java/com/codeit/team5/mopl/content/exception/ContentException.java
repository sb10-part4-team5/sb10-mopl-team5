package com.codeit.team5.mopl.content.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public abstract class ContentException extends BusinessException {

    public ContentException(HttpStatus status, String message) {
        super(status, message);
    }

    public ContentException(HttpStatus status, String message, Map<String, Object> details) {
        super(status, message, details);
    }
}
