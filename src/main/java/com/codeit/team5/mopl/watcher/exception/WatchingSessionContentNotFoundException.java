package com.codeit.team5.mopl.watcher.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class WatchingSessionContentNotFoundException extends BusinessException {

    public WatchingSessionContentNotFoundException(UUID contentId) {
        super(HttpStatus.NOT_FOUND, "컨텐츠를 찾을 수 없습니다.", Map.of("contentId", contentId));
    }
}
