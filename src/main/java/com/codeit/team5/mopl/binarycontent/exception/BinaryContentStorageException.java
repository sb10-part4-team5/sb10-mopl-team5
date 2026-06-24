package com.codeit.team5.mopl.binarycontent.exception;

import com.codeit.team5.mopl.global.exception.suggestion.BusinessExceptionSuggestion;
import org.springframework.http.HttpStatus;

public class BinaryContentStorageException extends BusinessExceptionSuggestion {

    public BinaryContentStorageException(HttpStatus status, String message) {
        super(status, message);
    }

}
