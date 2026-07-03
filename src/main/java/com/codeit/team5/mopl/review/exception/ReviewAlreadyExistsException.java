package com.codeit.team5.mopl.review.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class ReviewAlreadyExistsException extends BusinessException {

    public ReviewAlreadyExistsException() {
        super(HttpStatus.CONFLICT, "이미 해당 콘텐츠에 리뷰를 작성했습니다.");
    }
}
