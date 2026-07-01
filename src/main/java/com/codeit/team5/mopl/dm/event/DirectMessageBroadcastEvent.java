package com.codeit.team5.mopl.dm.event;

import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import java.util.UUID;

public record DirectMessageBroadcastEvent(
        UUID conversationId,
        DirectMessageResponse message
) {
}
