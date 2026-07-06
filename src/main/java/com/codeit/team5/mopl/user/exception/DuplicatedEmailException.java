package com.codeit.team5.mopl.user.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class DuplicatedEmailException extends UserException {

    public DuplicatedEmailException(String details) {
        super(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.", Map.of("email", details));
    }
}
