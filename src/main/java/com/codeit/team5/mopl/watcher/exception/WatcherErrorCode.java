package com.codeit.team5.mopl.watcher.exception;

import com.codeit.team5.mopl.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum WatcherErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "User를 찾을 수 없습니다."),
    CONTENT_NOT_FOUND(HttpStatus.BAD_REQUEST, "Content를 찾을 수 없습니다."),
    NOT_FOUND(HttpStatus.BAD_REQUEST, "User의 시청 세션을 찾을 수 없습니다."),
    ;

    private final HttpStatus status;
    private final String message;
}
