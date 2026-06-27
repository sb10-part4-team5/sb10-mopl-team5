package com.codeit.team5.mopl.notification.dto;

import com.codeit.team5.mopl.notification.entity.NotificationLevel;
import com.codeit.team5.mopl.notification.entity.NotificationType;
import java.time.Instant;
import java.util.UUID;

// 만일 REST 필드가 변경될 시 결합도를 낮추기 위한 페이로드
// 내부에서 알림 객체를 전달할 때 사용됨.
public record NotificationPayload(
    UUID notificationId,
    UUID receiverId,
    NotificationType type,
    String title,
    String content,
    NotificationLevel level,
    Instant createdAt
) {}

