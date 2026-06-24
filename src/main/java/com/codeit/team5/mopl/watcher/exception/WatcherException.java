package com.codeit.team5.mopl.watcher.exception;

import com.codeit.team5.mopl.global.exception.suggestion.BusinessExceptionSuggestion;
import java.util.Map;
import org.springframework.http.HttpStatus;

public abstract class WatcherException extends BusinessExceptionSuggestion {

    public WatcherException(HttpStatus status, String message) {
        super(status, message);
    }

    public WatcherException(HttpStatus status, String message, Map<String, Object> details) {
        super(status, message, details);
    }
}
