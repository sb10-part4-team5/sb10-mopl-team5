package com.codeit.team5.mopl.watcher.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;
import com.codeit.team5.mopl.global.exception.BusinessException;

public class WatchingSessionNotFoundException extends BusinessException {

    public WatchingSessionNotFoundException(Map<String, Object> details) {
        super(HttpStatus.NOT_FOUND, "User의 시청 세션을 찾을 수 없습니다.", details);
    }
}
