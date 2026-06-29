package com.codeit.team5.mopl.content.exception;

import java.util.Collections;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class ContentIncorrectSortByException extends ContentException {

    public ContentIncorrectSortByException(String sortBy) {
        super(HttpStatus.BAD_REQUEST, "SortBy 입력값이 올바르지 않습니다.",
                sortBy != null ? Map.of("incorrectSortBy", sortBy) : Collections.emptyMap());
    }
}
