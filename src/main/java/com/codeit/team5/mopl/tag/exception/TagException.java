package com.codeit.team5.mopl.tag.exception;

import com.codeit.team5.mopl.global.exception.suggestion.BusinessExceptionSuggestion;
import org.springframework.http.HttpStatus;

public class TagException extends BusinessExceptionSuggestion {

    public TagException(HttpStatus status, String message) {
        super(status, message);
    }
}
