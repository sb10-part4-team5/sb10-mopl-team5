package com.codeit.team5.mopl.user.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidUserSortByException extends BusinessException {

    public InvalidUserSortByException(String value) {
        super(HttpStatus.BAD_REQUEST, "정렬 입력값이 올바르지 않습니다. value=" + value );
    }
}
