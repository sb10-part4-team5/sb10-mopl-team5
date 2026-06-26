package com.codeit.team5.mopl.notification.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidNotificationTitleException extends BusinessException {

    public InvalidNotificationTitleException() {

        super(HttpStatus.BAD_REQUEST, "알림의 title 값이 유효하지 않습니다.");
    }
}
