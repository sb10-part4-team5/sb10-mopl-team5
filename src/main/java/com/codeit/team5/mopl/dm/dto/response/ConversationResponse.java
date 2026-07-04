package com.codeit.team5.mopl.dm.dto.response;

import com.codeit.team5.mopl.user.dto.response.UserSummaryResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        UserSummaryResponse with,
        @JsonProperty("lastestMessage") DirectMessageResponse latestMessage,
        boolean hasUnread
) {
}
