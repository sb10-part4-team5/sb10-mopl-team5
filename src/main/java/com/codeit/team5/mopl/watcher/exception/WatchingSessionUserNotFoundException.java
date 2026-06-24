package com.codeit.team5.mopl.watcher.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class WatchingSessionUserNotFoundException extends WatcherException {

    public WatchingSessionUserNotFoundException(UUID userId) {
        super(HttpStatus.NOT_FOUND, "User를 찾을 수 없습니다.", Map.of("userId", userId));
    }
}
