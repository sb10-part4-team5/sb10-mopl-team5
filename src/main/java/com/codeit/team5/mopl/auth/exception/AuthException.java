package com.codeit.team5.mopl.auth.exception;

import com.codeit.team5.mopl.global.exception.suggestion.BusinessExceptionSuggestion;
import java.util.Map;
import org.springframework.http.HttpStatus;

public abstract class AuthException extends BusinessExceptionSuggestion {

    public AuthException(HttpStatus status, String message) {
        super(status, message);
    }

    public AuthException(HttpStatus status, String message, Map<String, Object> details) {
        super(status, message, details);
    }
}
