package com.codeit.team5.mopl.notification.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidNotificationTypeException extends BusinessException {

    public InvalidNotificationTypeException() {
        super(HttpStatus.BAD_REQUEST, "알림의 type이 유효하지 않습니다.");
    }
}
