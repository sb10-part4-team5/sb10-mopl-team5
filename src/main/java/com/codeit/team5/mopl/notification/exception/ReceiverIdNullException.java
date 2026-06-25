package com.codeit.team5.mopl.notification.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class ReceiverIdNullException extends BusinessException {

    public ReceiverIdNullException() {
        super(HttpStatus.BAD_REQUEST, "수신자의 ID가 Null입니다.");
    }
}
