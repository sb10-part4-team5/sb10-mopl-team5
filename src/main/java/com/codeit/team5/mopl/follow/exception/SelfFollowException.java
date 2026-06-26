package com.codeit.team5.mopl.follow.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class SelfFollowException extends FollowException {

    public SelfFollowException(UUID userId) {
        super(HttpStatus.BAD_REQUEST, "자기 자신을 팔로우할 수 없습니다.", Map.of("userId", userId));
    }
}
