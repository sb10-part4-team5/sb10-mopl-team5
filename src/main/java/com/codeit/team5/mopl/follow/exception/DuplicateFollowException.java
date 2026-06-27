package com.codeit.team5.mopl.follow.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class DuplicateFollowException extends FollowException {

    public DuplicateFollowException(UUID followerId, UUID followeeId) {
        super(HttpStatus.CONFLICT, "이미 팔로우한 사용자입니다.",
                Map.of("followerId", followerId, "followeeId", followeeId));
    }
}
