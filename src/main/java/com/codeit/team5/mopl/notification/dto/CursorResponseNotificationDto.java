package com.codeit.team5.mopl.notification.dto;

import java.util.List;
import java.util.UUID;

public record CursorResponseNotificationDto(
    List<NotificationResponse> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    long totalCount,
    String sortBy,
    String sortDirection
) {

}
