package com.codeit.team5.mopl.review.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidRatingException extends BusinessException {

    public InvalidRatingException(Double rating) {

        super(HttpStatus.BAD_REQUEST, "평점은 0.0 이상, 5.0 이하여야 합니다. rating: "+ rating);
    }
}
