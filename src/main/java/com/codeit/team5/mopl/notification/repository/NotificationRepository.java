package com.codeit.team5.mopl.notification.repository;

import com.codeit.team5.mopl.notification.entity.Notification;
import com.codeit.team5.mopl.notification.repository.querydsl.NotificationQueryRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID>, NotificationQueryRepository {

    long countByReceiverId(UUID receiverId);

    long countByReceiverIdAndIsReadFalse(UUID receiverId);

    Optional<Notification> findByIdAndReceiverId(UUID id, UUID receiverId);
}
