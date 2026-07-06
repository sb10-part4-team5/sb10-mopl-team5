package com.codeit.team5.mopl.review.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class ReviewForbiddenException extends BusinessException {

    public ReviewForbiddenException() {
        super(HttpStatus.FORBIDDEN, "리뷰를 수정/삭제할 권한이 없습니다.");
    }
}
