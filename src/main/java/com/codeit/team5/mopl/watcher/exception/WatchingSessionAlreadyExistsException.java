package com.codeit.team5.mopl.watcher.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import com.codeit.team5.mopl.global.exception.BusinessException;

public class WatchingSessionAlreadyExistsException extends BusinessException {

    public WatchingSessionAlreadyExistsException(UUID watcherId) {
        super(HttpStatus.BAD_REQUEST, "이미 시청 중인 컨텐츠가 있습니다.", Map.of("watcherId", watcherId));
    }

}
