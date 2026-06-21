package com.codeit.team5.mopl.notification.mapper;

import com.codeit.team5.mopl.notification.dto.NotificationResponse;
import com.codeit.team5.mopl.notification.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toDto(Notification notification);

    Notification toNotification(Notification notification);

}
