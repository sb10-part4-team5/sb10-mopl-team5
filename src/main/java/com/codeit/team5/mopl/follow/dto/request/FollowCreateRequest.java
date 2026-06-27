package com.codeit.team5.mopl.follow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(name = "FollowRequest", description = "팔로우 요청")
public record FollowCreateRequest(
        @Schema(description = "팔로우 대상 사용자 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "followeeId는 필수입니다.")
        UUID followeeId
) {
}
