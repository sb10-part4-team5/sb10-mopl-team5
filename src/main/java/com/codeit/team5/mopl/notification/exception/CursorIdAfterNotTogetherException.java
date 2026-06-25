package com.codeit.team5.mopl.notification.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class CursorIdAfterNotTogetherException extends BusinessException {


    public CursorIdAfterNotTogetherException() {

        super(HttpStatus.BAD_REQUEST, "cursor와 idAfter가 같이 제공되지 않습니다.");
    }
}
