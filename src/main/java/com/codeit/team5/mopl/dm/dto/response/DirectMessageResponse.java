package com.codeit.team5.mopl.dm.dto.response;

import com.codeit.team5.mopl.user.dto.response.UserSummaryResponse;
import java.time.Instant;
import java.util.UUID;

public record DirectMessageResponse(
        UUID id,
        UUID conversationId,
        UserSummaryResponse sender,
        UserSummaryResponse receiver,
        String content,
        Instant createdAt
) {
}
