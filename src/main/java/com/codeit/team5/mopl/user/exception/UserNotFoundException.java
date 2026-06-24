package com.codeit.team5.mopl.user.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends UserExceptionSuggestion {

    public UserNotFoundException(UUID userId) {
        super(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.", Map.of("userId", userId));
    }
}
