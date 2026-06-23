package com.codeit.team5.mopl.watcher.exception;

import com.codeit.team5.mopl.global.exception.suggestion.ErrorCodeSuggestion;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum WatcherErrorCode implements ErrorCodeSuggestion {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User를 찾을 수 없습니다."),
    CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Content를 찾을 수 없습니다."),
    WATCHING_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "User의 시청 세션을 찾을 수 없습니다."),
    ;

    private final HttpStatus status;
    private final String message;
}
