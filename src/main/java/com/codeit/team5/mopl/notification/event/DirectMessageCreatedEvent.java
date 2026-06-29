package com.codeit.team5.mopl.notification.event;

import com.codeit.team5.mopl.sse.dto.DirectMessagePayload;

public record DirectMessageCreatedEvent(
    DirectMessagePayload directMessagePayload
) {
}
