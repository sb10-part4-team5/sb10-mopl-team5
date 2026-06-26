package com.codeit.team5.mopl.follow.dto.response;

import java.util.UUID;

public record FollowResponse(
        UUID id,
        UUID followeeId,
        UUID followerId
) {
}
