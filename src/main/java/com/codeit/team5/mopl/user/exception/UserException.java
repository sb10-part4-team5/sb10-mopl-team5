package com.codeit.team5.mopl.user.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public abstract class UserException extends BusinessException {

    public UserException(HttpStatus status, String message) {
        super(status, message);
    }

    public UserException(HttpStatus status, String message, Map<String, Object> details) {
        super(status, message, details);
    }
}
