package com.codeit.team5.mopl.user.exception;

import org.springframework.http.HttpStatus;

public class InvalidUsernameException extends UserException {

    public InvalidUsernameException() {
        super(HttpStatus.BAD_REQUEST, "사용자 이름은 공백일 수 없습니다.");
    }
}
