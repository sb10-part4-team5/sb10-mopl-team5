package com.codeit.team5.mopl.follow.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class FollowNotFoundException extends FollowException {

    public FollowNotFoundException(UUID followId) {
        super(HttpStatus.NOT_FOUND, "팔로우가 존재하지 않습니다.", Map.of("followId", followId));
    }

    public FollowNotFoundException(UUID followerId, UUID followeeId) {
        super(HttpStatus.NOT_FOUND, "팔로우가 존재하지 않습니다.",
                Map.of("followerId", followerId, "followeeId", followeeId));
    }
}
