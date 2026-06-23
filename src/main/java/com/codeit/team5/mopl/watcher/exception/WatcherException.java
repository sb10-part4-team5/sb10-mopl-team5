package com.codeit.team5.mopl.watcher.exception;

import com.codeit.team5.mopl.global.exception.suggestion.BusinessExceptionSuggestion;
import com.codeit.team5.mopl.global.exception.suggestion.ErrorCodeSuggestion;
import java.util.Map;

public class WatcherException extends BusinessExceptionSuggestion {

    public WatcherException(ErrorCodeSuggestion errorCode) {
        super(errorCode);
    }

    public WatcherException(ErrorCodeSuggestion errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }
}
