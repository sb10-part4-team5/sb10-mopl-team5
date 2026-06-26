package com.codeit.team5.mopl.notification.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeit.team5.mopl.notification.exception.InvalidNotificationTitleException;
import com.codeit.team5.mopl.notification.exception.InvalidNotificationTypeException;
import com.codeit.team5.mopl.notification.exception.ReceiverIdNullException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationTest {

    @Test
    @DisplayName("알림 생성에 성공한다")
    void create_success() {
        // given
        UUID receiverId = UUID.randomUUID();

        // when
        Notification notification = Notification.create(
                receiverId, NotificationType.FOLLOWED, "제목", "내용", NotificationLevel.WARNING);

        // then
        assertThat(notification.getReceiverId()).isEqualTo(receiverId);
        assertThat(notification.getType()).isEqualTo(NotificationType.FOLLOWED);
        assertThat(notification.getTitle()).isEqualTo("제목");
        assertThat(notification.getContent()).isEqualTo("내용");
        assertThat(notification.getLevel()).isEqualTo(NotificationLevel.WARNING);
        assertThat(notification.isRead()).isFalse();
        assertThat(notification.getReadAt()).isNull();
    }

    @Test
    @DisplayName("level이 null이면 INFO로 기본 설정된다")
    void create_levelNull_INFO() {
        // when
        Notification notification = Notification.create(
                UUID.randomUUID(), NotificationType.FOLLOWED, "제목", "내용", null);

        // then
        assertThat(notification.getLevel()).isEqualTo(NotificationLevel.INFO);
    }

    @Test
    @DisplayName("receiverId가 null이면 예외가 발생한다")
    void create_receiverIdNull_exception() {
        // when & then
        assertThatThrownBy(() -> Notification.create(
                null, NotificationType.FOLLOWED, "제목", "내용", NotificationLevel.INFO))
                .isInstanceOf(ReceiverIdNullException.class);
    }

    @Test
    @DisplayName("type이 null이면 예외가 발생한다")
    void create_typeNull_exception() {
        // when & then
        assertThatThrownBy(() -> Notification.create(
                UUID.randomUUID(), null, "제목", "내용", NotificationLevel.INFO))
                .isInstanceOf(InvalidNotificationTypeException.class);
    }

    @Test
    @DisplayName("title이 비어 있으면 예외가 발생한다")
    void create_titleBlank_exception() {
        // when & then
        assertThatThrownBy(() -> Notification.create(
                UUID.randomUUID(), NotificationType.FOLLOWED, "  ", "내용", NotificationLevel.INFO))
                .isInstanceOf(InvalidNotificationTitleException.class);
    }

    @Test
    @DisplayName("title이 null이면 예외가 발생한다")
    void create_titleNull_exception() {
        // when & then
        assertThatThrownBy(() -> Notification.create(
                UUID.randomUUID(), NotificationType.FOLLOWED, null, "내용", NotificationLevel.INFO))
                .isInstanceOf(InvalidNotificationTitleException.class);
    }

    @Test
    @DisplayName("읽음 처리하면 isRead가 true가 되고 readAt이 기록된다")
    void markAsRead_success() {
        // given
        Notification notification = Notification.create(
                UUID.randomUUID(), NotificationType.FOLLOWED, "제목", "내용", NotificationLevel.INFO);

        // when
        notification.markAsRead();

        // then
        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 읽은 알림을 다시 읽음 처리해도 readAt이 변경되지 않는다")
    void markAsRead_read_readAt() {
        // given
        Notification notification = Notification.create(
                UUID.randomUUID(), NotificationType.FOLLOWED, "제목", "내용", NotificationLevel.INFO);
        notification.markAsRead();
        Instant firstReadAt = notification.getReadAt();

        // when
        notification.markAsRead();

        // then
        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isEqualTo(firstReadAt);
    }
}
