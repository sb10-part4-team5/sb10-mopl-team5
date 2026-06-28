package com.codeit.team5.mopl.content.exception;

import org.springframework.http.HttpStatus;

public class CursorIdAfterNotTogetherException extends ContentException {

    public CursorIdAfterNotTogetherException() {
        super(HttpStatus.BAD_REQUEST, "cursor와 idAfter가 같이 제공되지 않습니다.");
    }
}
