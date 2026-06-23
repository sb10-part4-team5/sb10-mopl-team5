package com.codeit.team5.mopl.global.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

    HttpStatus getStatus();

    String getMessage();
}
