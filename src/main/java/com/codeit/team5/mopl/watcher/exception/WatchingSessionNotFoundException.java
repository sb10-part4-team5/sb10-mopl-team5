package com.codeit.team5.mopl.watcher.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class WatchingSessionNotFoundException extends WatcherException {

    public WatchingSessionNotFoundException(String name, Object value) {
        this(Map.of(name, value));
    }

    public WatchingSessionNotFoundException(Map<String, Object> details) {
        super(HttpStatus.NOT_FOUND, "User의 시청 세션을 찾을 수 없습니다.", details);
    }
}
