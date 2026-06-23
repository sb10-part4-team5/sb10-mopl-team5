package com.codeit.team5.mopl.global.exception.suggestion;

import org.springframework.http.HttpStatus;

public interface ErrorCodeSuggestion {

    HttpStatus getStatus();

    String getMessage();
}
