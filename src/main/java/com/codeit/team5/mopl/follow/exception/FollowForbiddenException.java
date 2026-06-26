package com.codeit.team5.mopl.follow.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class FollowForbiddenException extends FollowException {

    public FollowForbiddenException(UUID followId, UUID requesterId) {
        super(HttpStatus.FORBIDDEN, "본인의 팔로우만 취소할 수 있습니다.",
                Map.of("followId", followId, "requesterId", requesterId));
    }
}
