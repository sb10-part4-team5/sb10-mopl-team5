package com.codeit.team5.mopl.dm.exception;

import org.springframework.http.HttpStatus;

public class InvalidDirectMessageContentException extends DmException {

    public InvalidDirectMessageContentException() {
        super(HttpStatus.BAD_REQUEST, "메시지 내용은 비어 있을 수 없습니다.");
    }
}
