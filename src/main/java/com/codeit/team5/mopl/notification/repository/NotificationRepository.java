package com.codeit.team5.mopl.notification.repository;

import com.codeit.team5.mopl.notification.entity.Notification;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

}
