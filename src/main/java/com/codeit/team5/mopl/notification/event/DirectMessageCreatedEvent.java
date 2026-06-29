package com.codeit.team5.mopl.notification.event;

import com.codeit.team5.mopl.notification.dto.NotificationPayload;

public record DirectMessageCreatedEvent(
    NotificationPayload notificationPayload
) {
}
