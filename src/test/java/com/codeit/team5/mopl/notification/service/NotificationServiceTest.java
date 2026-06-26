package com.codeit.team5.mopl.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.codeit.team5.mopl.notification.dto.CursorResponseNotificationDto;
import com.codeit.team5.mopl.notification.dto.NotificationResponse;
import com.codeit.team5.mopl.notification.entity.Notification;
import com.codeit.team5.mopl.notification.entity.NotificationLevel;
import com.codeit.team5.mopl.notification.entity.NotificationType;
import com.codeit.team5.mopl.notification.exception.NotificationNotFoundException;
import com.codeit.team5.mopl.notification.mapper.NotificationMapper;
import com.codeit.team5.mopl.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("알림 생성에 성공한다")
    void create_success() {
        // given
        UUID receiverId = UUID.randomUUID();
        Notification saved = mock(Notification.class);
        NotificationResponse response = mock(NotificationResponse.class);
        given(notificationRepository.save(any(Notification.class))).willReturn(saved);
        given(notificationMapper.toResponse(saved)).willReturn(response);

        // when
        NotificationResponse result = notificationService.create(
                receiverId, NotificationType.FOLLOWED, "제목", "내용", NotificationLevel.INFO);

        // then
        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("다음 페이지가 있으면 limit만큼 잘라내고 nextCursor를 채운다")
    void getNotifications_hasNext() {
        // given
        UUID receiverId = UUID.randomUUID();
        Instant lastCreatedAt = Instant.parse("2026-06-26T00:00:00Z");
        UUID lastId = UUID.randomUUID();

        Notification n0 = mock(Notification.class);
        Notification n1 = mock(Notification.class);
        Notification n2 = mock(Notification.class);
        // 페이지 마지막 요소(n1)에서 커서를 추출
        given(n1.getCreatedAt()).willReturn(lastCreatedAt);
        given(n1.getId()).willReturn(lastId);

        // limit=2 → limit+1(=3)개 조회되어 hasNext=true
        given(notificationRepository.findPageByReceiverDesc(eq(receiverId), eq(null), eq(null), eq(Limit.of(3))))
                .willReturn(List.of(n0, n1, n2));
        given(notificationRepository.countByReceiverId(receiverId)).willReturn(10L);
        given(notificationMapper.toResponseList(anyList())).willReturn(List.of());

        // when
        CursorResponseNotificationDto result = notificationService.getNotifications(
                receiverId, null, null, 2, "DESCENDING", "createdAt");

        // then
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(lastCreatedAt.toString());
        assertThat(result.nextIdAfter()).isEqualTo(lastId);
        assertThat(result.totalCount()).isEqualTo(10L);
    }

    @Test
    @DisplayName("마지막 페이지면 hasNext가 false이고 nextCursor가 null이다")
    void getNotifications_lastPage() {
        // given
        UUID receiverId = UUID.randomUUID();
        Notification n0 = mock(Notification.class);
        // limit=2, 조회 결과 2개(<= limit) → hasNext=false
        given(notificationRepository.findPageByReceiverDesc(eq(receiverId), eq(null), eq(null), eq(Limit.of(3))))
                .willReturn(List.of(n0));
        given(notificationRepository.countByReceiverId(receiverId)).willReturn(1L);
        given(notificationMapper.toResponseList(anyList())).willReturn(List.of());

        // when
        CursorResponseNotificationDto result = notificationService.getNotifications(
                receiverId, null, null, 2, "DESCENDING", "createdAt");

        // then
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
        assertThat(result.nextIdAfter()).isNull();
    }

    @Test
    @DisplayName("정렬 방향이 ASCENDING이면 오름차순 조회 메서드를 호출한다")
    void getNotifications_ascending() {
        // given
        UUID receiverId = UUID.randomUUID();
        given(notificationRepository.findPageByReceiverAsc(eq(receiverId), eq(null), eq(null), eq(Limit.of(3))))
                .willReturn(List.of());
        given(notificationRepository.countByReceiverId(receiverId)).willReturn(0L);
        given(notificationMapper.toResponseList(anyList())).willReturn(List.of());

        // when
        CursorResponseNotificationDto result = notificationService.getNotifications(
                receiverId, null, null, 2, "ASCENDING", "createdAt");

        // then
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("안 읽은 알림 개수를 반환한다")
    void countUnread_success() {
        // given
        UUID receiverId = UUID.randomUUID();
        given(notificationRepository.countByReceiverIdAndIsReadFalse(receiverId)).willReturn(5L);

        // when
        long result = notificationService.countUnread(receiverId);

        // then
        assertThat(result).isEqualTo(5L);
    }

    @Test
    @DisplayName("읽음 처리에 성공하면 알림의 markAsRead가 호출된다")
    void markAsRead_success() {
        // given
        UUID notificationId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        Notification notification = mock(Notification.class);
        NotificationResponse response = mock(NotificationResponse.class);
        given(notificationRepository.findByIdAndReceiverId(notificationId, receiverId))
                .willReturn(Optional.of(notification));
        given(notificationMapper.toResponse(notification)).willReturn(response);

        // when
        NotificationResponse result = notificationService.markAsRead(notificationId, receiverId);

        // then
        assertThat(result).isEqualTo(response);
        org.mockito.Mockito.verify(notification).markAsRead();
    }

    @Test
    @DisplayName("존재하지 않거나 소유자가 아니면 읽음 처리 시 예외가 발생한다")
    void markAsRead_notFound_exception() {
        // given
        UUID notificationId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        given(notificationRepository.findByIdAndReceiverId(notificationId, receiverId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationService.markAsRead(notificationId, receiverId))
                .isInstanceOf(NotificationNotFoundException.class);
    }
}
