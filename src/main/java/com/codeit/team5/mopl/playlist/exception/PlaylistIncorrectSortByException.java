package com.codeit.team5.mopl.playlist.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class PlaylistIncorrectSortByException extends BusinessException {

    public PlaylistIncorrectSortByException(String sortBy) {
        super(HttpStatus.BAD_REQUEST, "SortBy 입력값이 올바르지 않습니다.", Map.of("incorrectSortBy", sortBy));
    }
}
