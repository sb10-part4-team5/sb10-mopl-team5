package com.codeit.team5.mopl.notification.event;

import com.codeit.team5.mopl.notification.dto.NotificationResponse;

public record NotificationCreatedEvent (
    NotificationResponse notification
){

}
