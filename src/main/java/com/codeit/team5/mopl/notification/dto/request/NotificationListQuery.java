package com.codeit.team5.mopl.notification.dto.request;

import java.util.UUID;

public record NotificationListQuery(
    UUID receiverId,
    String cursor,
    UUID idAfter,
    int limit,
    String sortDirection,
    String sortBy
) {

}
