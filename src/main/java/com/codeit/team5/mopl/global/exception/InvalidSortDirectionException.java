package com.codeit.team5.mopl.global.exception;

import org.springframework.http.HttpStatus;

public class InvalidSortDirectionException extends BusinessException {

    public InvalidSortDirectionException(String value) {
        super(HttpStatus.BAD_REQUEST, "정렬 방향 입력값이 올바르지 않습니다. (ASCENDING 또는 DESCENDING): " + value);
    }
}
