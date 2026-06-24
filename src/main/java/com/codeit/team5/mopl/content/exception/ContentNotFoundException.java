package com.codeit.team5.mopl.content.exception;

import org.springframework.http.HttpStatus;

public class ContentNotFoundException extends ContentException {

    public ContentNotFoundException() {
        super(HttpStatus.NOT_FOUND, "콘텐츠를 찾을 수 없습니다.");
    }
}
