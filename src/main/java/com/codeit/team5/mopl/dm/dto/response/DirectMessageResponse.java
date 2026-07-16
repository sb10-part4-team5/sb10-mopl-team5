package com.codeit.team5.mopl.dm.dto.response;

import com.codeit.team5.mopl.user.dto.response.UserSummary;
import java.time.Instant;
import java.util.UUID;

public record DirectMessageResponse(
        UUID id,
        UUID conversationId,
        UserSummary sender,
        UserSummary receiver,
        String content,
        Instant createdAt
) {
}
