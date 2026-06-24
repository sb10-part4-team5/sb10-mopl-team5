package com.codeit.team5.mopl.watcher.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class WatchingSessionNotFoundException extends WatcherException {

    public WatchingSessionNotFoundException(UUID watcherId) {
        super(HttpStatus.NOT_FOUND, "User의 시청 세션을 찾을 수 없습니다.", Map.of("watcherId", watcherId));
    }
}
