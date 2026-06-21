package com.codeit.team5.mopl.notification.entity;

import com.codeit.team5.mopl.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
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

    // 생성자
    public Notification(UUID receiverId, String title, String content, NotificationLevel level) {
        this.receiverId = receiverId;
        this.title = title;
        this.content = content;
        this.level = level != null ? level : NotificationLevel.INFO;
        this.isRead = false;
    }

    // 알림 읽기 처리 메서드
    public void markAsRead() {
        this.isRead = true;
    }
}
