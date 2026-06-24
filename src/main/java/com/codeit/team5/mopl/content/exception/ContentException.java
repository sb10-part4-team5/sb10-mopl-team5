package com.codeit.team5.mopl.content.exception;

import com.codeit.team5.mopl.global.exception.suggestion.BusinessExceptionSuggestion;
import org.springframework.http.HttpStatus;

public class ContentException extends BusinessExceptionSuggestion {

    public ContentException(HttpStatus status, String message) {
        super(status, message);
    }
}
