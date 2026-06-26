package com.codeit.team5.mopl.notification.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidCursorException extends BusinessException {

    public InvalidCursorException(String cursor){
        super(HttpStatus.BAD_REQUEST, "커서 값이 유효하지 않습니다. cursor={" + cursor + "}");
    }
}
