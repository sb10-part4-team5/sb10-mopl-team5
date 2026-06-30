package com.codeit.team5.mopl.user.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class UserForbiddenException extends UserException {

    public UserForbiddenException(UUID requesterId, UUID userId) {
        super(HttpStatus.FORBIDDEN, "본인의 프로필만 수정할 수 있습니다.",
                Map.of("requesterId", requesterId, "userId", userId));
    }
}
