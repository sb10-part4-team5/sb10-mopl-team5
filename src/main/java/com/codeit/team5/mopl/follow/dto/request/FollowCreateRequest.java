package com.codeit.team5.mopl.follow.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record FollowCreateRequest(
        @NotNull(message = "followeeId는 필수입니다.")
        UUID followeeId
) {
}
