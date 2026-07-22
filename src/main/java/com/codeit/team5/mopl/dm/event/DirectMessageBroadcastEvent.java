package com.codeit.team5.mopl.dm.event;

import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.global.outbox.event.RetryableOutboxEvent;
import java.util.UUID;

public record DirectMessageBroadcastEvent(
    DirectMessageResponse message,
    UUID receiverId
) implements RetryableOutboxEvent {
}
