package com.codeit.team5.mopl.dm.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public abstract class DmException extends BusinessException {

    public DmException(HttpStatus status, String message) {
        super(status, message);
    }

    public DmException(HttpStatus status, String message, Map<String, Object> details) {
        super(status, message, details);
    }
}
