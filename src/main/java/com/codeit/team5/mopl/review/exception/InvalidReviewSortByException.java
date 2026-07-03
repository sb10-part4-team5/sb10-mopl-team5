package com.codeit.team5.mopl.review.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidReviewSortByException extends BusinessException {

    public InvalidReviewSortByException(String sortBy) {
        super(HttpStatus.BAD_REQUEST, "지원하지 않는 정렬 기준입니다 (createdAt 또는 rating): " + sortBy);
    }
}
