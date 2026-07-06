package com.codeit.team5.mopl.dm.event;

import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;

public record DirectMessageNotificationEvent(
        DirectMessageResponse message
) {
}
