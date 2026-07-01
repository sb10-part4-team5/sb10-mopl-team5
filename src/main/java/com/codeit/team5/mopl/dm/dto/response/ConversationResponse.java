package com.codeit.team5.mopl.dm.dto.response;

import com.codeit.team5.mopl.user.dto.response.UserSummaryResponse;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        UserSummaryResponse with,
        DirectMessageResponse latestMessage,
        boolean hasUnread
) {
}
