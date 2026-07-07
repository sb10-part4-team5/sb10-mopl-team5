package com.codeit.team5.mopl.notification.dto.request;

import com.codeit.team5.mopl.notification.entity.NotificationLevel;
import com.codeit.team5.mopl.notification.entity.NotificationType;
import java.util.List;
import java.util.UUID;

public record NotificationBatchCreateCommand(
    List<UUID> receiverIds,
    NotificationType type,
    String title,
    String content,
    NotificationLevel level
) {

}
