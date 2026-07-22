package com.codeit.team5.mopl.dm.event;

import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.global.outbox.event.RetryableOutboxEvent;

public record DirectMessageNotificationEvent(
        DirectMessageResponse message
) implements RetryableOutboxEvent {
}
