package com.codeit.team5.mopl.notification.entity;

import com.codeit.team5.mopl.global.entity.BaseEntity;
import com.codeit.team5.mopl.notification.exception.InvalidNotificationTitleException;
import com.codeit.team5.mopl.notification.exception.InvalidNotificationTypeException;
import com.codeit.team5.mopl.notification.exception.ReceiverIdNullException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "notifications")
@Getter
@ToString(of = {"title", "level", "isRead"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    // 수신자 ID
    @Column(name = "receiver_id", nullable = false, columnDefinition = "uuid")
    private UUID receiverId;

    // 알림 종류
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    // 알림 제목
    @Column(nullable = false, length = 255)
    private String title;

    // 알림 내용
    @Column(columnDefinition = "TEXT")
    private String content;

    // 알림 LEVEL
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationLevel level;

    // 알림 읽기 여부
    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    // 읽은 시각
    @Column(name = "read_at", nullable = true)
    private Instant readAt;

    public static Notification create(
        UUID receiverId, NotificationType type, String title, String content,
        NotificationLevel level) {
        if (receiverId == null) {
            throw new ReceiverIdNullException();
        }
        if (type == null) {
            throw new InvalidNotificationTypeException();
        }
        if (title == null || title.isBlank()) {
            throw new InvalidNotificationTitleException();
        }
        return new Notification(receiverId, type, title, content, level);
    }

    // 생성자
    private Notification(UUID receiverId, NotificationType type, String title, String content,
        NotificationLevel level) {
        this.receiverId = receiverId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.level = level != null ? level : NotificationLevel.INFO;
        this.isRead = false;
        this.readAt = null;
    }

    // 알림 읽기 처리 메서드
    public void markAsRead() {
        if(this.isRead) {
            return;
        }
        this.isRead = true;
        this.readAt = Instant.now();
    }
}
