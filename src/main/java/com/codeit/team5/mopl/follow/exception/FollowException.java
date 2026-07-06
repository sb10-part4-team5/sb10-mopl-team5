package com.codeit.team5.mopl.follow.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public abstract class FollowException extends BusinessException {

    public FollowException(HttpStatus status, String message) {
        super(status, message);
    }

    public FollowException(HttpStatus status, String message, Map<String, Object> details) {
        super(status, message, details);
    }
}
