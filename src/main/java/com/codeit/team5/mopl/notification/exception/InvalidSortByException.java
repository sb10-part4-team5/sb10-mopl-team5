package com.codeit.team5.mopl.notification.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidSortByException extends BusinessException {

    public InvalidSortByException(String sortBy) {
        super(HttpStatus.BAD_REQUEST, "지원하지 않는 정렬 기준입니다: sortBy=" + sortBy);
    }
}
