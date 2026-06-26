package com.codeit.team5.mopl.notification.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidSortDirectionException extends BusinessException {

    public InvalidSortDirectionException(String sortDirection) {
        super(HttpStatus.BAD_REQUEST, "지원하지 않는 정렬 방향입니다: sortDirection=" + sortDirection);
    }
}
