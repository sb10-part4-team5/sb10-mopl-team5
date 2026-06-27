package com.codeit.team5.mopl.notification.mapper;

import com.codeit.team5.mopl.notification.dto.NotificationPayload;
import com.codeit.team5.mopl.notification.dto.NotificationResponse;
import com.codeit.team5.mopl.notification.entity.Notification;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);

    List<NotificationResponse> toResponseList(List<Notification> notifications);

    NotificationPayload toPayload(Notification notification);

}
