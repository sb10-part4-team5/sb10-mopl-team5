package com.codeit.team5.mopl.notification.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidReceiverIdException extends BusinessException {

    public InvalidReceiverIdException() {
        super(HttpStatus.BAD_REQUEST, "수신자 ID가 유효하지 않음.");
    }
}
