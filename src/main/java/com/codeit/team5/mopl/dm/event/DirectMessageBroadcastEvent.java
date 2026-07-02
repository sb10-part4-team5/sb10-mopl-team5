package com.codeit.team5.mopl.dm.event;

import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;

public record DirectMessageBroadcastEvent(
        DirectMessageResponse message,
        String receiverEmail
) {
}
