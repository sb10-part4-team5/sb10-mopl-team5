package com.codeit.team5.mopl.dm.dto.response;

import com.codeit.team5.mopl.user.dto.response.UserSummaryResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        UserSummaryResponse with,
        @JsonProperty("lastestMessage") // 프론트 오타(latestMessage → lastestMessage)에 맞춰 JSON 키를 강제 지정
        DirectMessageResponse latestMessage,
        boolean hasUnread
) {
}
