package com.codeit.team5.mopl.watcher.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import com.codeit.team5.mopl.global.exception.ErrorCode;
import java.util.Map;

public class WatcherException extends BusinessException {

    public WatcherException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }
}
