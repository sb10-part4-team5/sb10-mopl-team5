package com.codeit.team5.mopl.notification.dto.request;

import com.codeit.team5.mopl.notification.entity.NotificationLevel;
import com.codeit.team5.mopl.notification.entity.NotificationType;
import java.util.UUID;

public record NotificationCreateCommand(
    UUID receiverId,
    NotificationType type,
    String title,
    String content,
    NotificationLevel level
) {

}
