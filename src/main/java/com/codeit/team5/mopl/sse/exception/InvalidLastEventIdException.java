package com.codeit.team5.mopl.sse.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidLastEventIdException extends BusinessException {

    public InvalidLastEventIdException() {
        super(HttpStatus.BAD_REQUEST, "Last-Event-ID가 유효하지 않습니다.");
    }
}
