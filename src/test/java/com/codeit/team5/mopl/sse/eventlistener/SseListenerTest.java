package com.codeit.team5.mopl.sse.eventlistener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.dm.event.DirectMessageSseEvent;
import com.codeit.team5.mopl.dm.fixture.DirectMessageTestFixtures;
import com.codeit.team5.mopl.notification.dto.NotificationPayload;
import com.codeit.team5.mopl.notification.entity.NotificationLevel;
import com.codeit.team5.mopl.notification.entity.NotificationType;
import com.codeit.team5.mopl.notification.event.NotificationCreatedEvent;
import com.codeit.team5.mopl.notification.event.NotificationsBatchCreatedEvent;
import com.codeit.team5.mopl.sse.sender.SseSender;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class SseListenerTest {

    @Mock
    private SseSender sseSender;

    @InjectMocks
    private SseListener listener;

    @Test
    @DisplayName("NotificationCreatedEvent 발생 시 수신자에게 notifications 이벤트를 전송한다")
    void onNotificationCreated_delegatesToSseSender() {
        // given
        UUID receiverId = UUID.randomUUID();
        NotificationPayload payload = notificationPayload(receiverId);

        // when
        listener.onNotificationCreated(new NotificationCreatedEvent(payload));

        // then
        verify(sseSender).sendToUser(eq(receiverId), any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("NotificationsBatchCreatedEvent 발생 시 각 수신자에게 notifications 이벤트를 전송한다")
    void onNotificationBatchCreated_delegatesToSseSender_forEachPayload() {
        // given
        UUID receiverId1 = UUID.randomUUID();
        UUID receiverId2 = UUID.randomUUID();
        NotificationPayload payload1 = notificationPayload(receiverId1);
        NotificationPayload payload2 = notificationPayload(receiverId2);

        // when
        listener.onNotificationBatchCreated(new NotificationsBatchCreatedEvent(List.of(payload1, payload2)));

        // then
        verify(sseSender).sendToUser(eq(receiverId1), any(SseEmitter.SseEventBuilder.class));
        verify(sseSender).sendToUser(eq(receiverId2), any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("배치 이벤트 페이로드가 비어 있으면 sseSender를 호출하지 않는다")
    void onNotificationBatchCreated_emptyList_doesNotCallSseSender() {
        // given (empty list)

        // when
        listener.onNotificationBatchCreated(new NotificationsBatchCreatedEvent(List.of()));

        // then
        verify(sseSender, never()).sendToUser(any(), any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("비활성 DM SSE 이벤트 발생 시 수신자에게 direct-messages 이벤트를 전송한다")
    void onDirectMessageSse_delegatesToSseSender() {
        // given
        UUID receiverId = UUID.randomUUID();
        DirectMessageResponse message = dmMessage(receiverId);

        // when
        listener.onDirectMessageSse(new DirectMessageSseEvent(message));

        // then
        verify(sseSender).sendToUser(eq(receiverId), any(SseEmitter.SseEventBuilder.class));
    }

    // ===== 헬퍼 =====

    private NotificationPayload notificationPayload(UUID receiverId) {
        return new NotificationPayload(
                UUID.randomUUID(), receiverId, NotificationType.FOLLOWED,
                "팔로우 알림", "내용", NotificationLevel.INFO, Instant.now());
    }

    private DirectMessageResponse dmMessage(UUID receiverId) {
        return DirectMessageTestFixtures.dmMessage(receiverId);
    }
}
