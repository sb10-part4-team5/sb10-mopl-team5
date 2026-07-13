package com.codeit.team5.mopl.notification.repository.querydsl;

import com.codeit.team5.mopl.notification.entity.Notification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;

public interface NotificationQueryRepository {

    List<Notification> findPageByReceiverDesc(UUID receiverId, Instant cursor, UUID idAfter, Limit limit);

    List<Notification> findPageByReceiverAsc(UUID receiverId, Instant cursor, UUID idAfter, Limit limit);

    List<Notification> findMissedNotifications(UUID receiverId, UUID lastEventId);
}
