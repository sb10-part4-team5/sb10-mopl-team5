package com.codeit.team5.mopl.user.exception;

import com.codeit.team5.mopl.global.exception.suggestion.BusinessExceptionSuggestion;
import java.util.Map;
import org.springframework.http.HttpStatus;

public abstract class UserExceptionSuggestion extends BusinessExceptionSuggestion {

    public UserExceptionSuggestion(HttpStatus status, String message) {
        super(status, message);
    }

    public UserExceptionSuggestion(HttpStatus status, String message, Map<String, Object> details) {
        super(status, message, details);
    }
}
