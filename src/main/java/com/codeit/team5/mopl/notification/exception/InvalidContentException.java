package com.codeit.team5.mopl.notification.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidContentException extends BusinessException {

    public InvalidContentException() {
        super(HttpStatus.BAD_REQUEST, "content가 유효하지 않음");
    }
}
