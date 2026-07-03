package com.codeit.team5.mopl.review.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class ReviewNotFoundException extends BusinessException {

    public ReviewNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다: id=" + id);
    }
}
