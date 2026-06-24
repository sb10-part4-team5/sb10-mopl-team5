package com.codeit.team5.mopl.watcher.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class WatchingSessionIncorrectSortByException extends WatcherException {

    public WatchingSessionIncorrectSortByException(String sortBy) {
        super(HttpStatus.BAD_REQUEST, "SortBy 입력값이 올바르지 않습니다.", Map.of("incorrectSortBy", sortBy));
    }
}
