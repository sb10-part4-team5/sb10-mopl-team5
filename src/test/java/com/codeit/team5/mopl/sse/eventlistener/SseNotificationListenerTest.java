package com.codeit.team5.mopl.sse.eventlistener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.notification.dto.NotificationPayload;
import com.codeit.team5.mopl.notification.entity.NotificationLevel;
import com.codeit.team5.mopl.notification.entity.NotificationType;
import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.dm.event.InactiveDirectMessageEvent;
import com.codeit.team5.mopl.notification.event.NotificationCreatedEvent;
import com.codeit.team5.mopl.sse.sender.SseSender;
import com.codeit.team5.mopl.user.dto.response.UserSummaryResponse;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class SseNotificationListenerTest {

    @Mock
    private SseSender sseSender;

    @InjectMocks
    private SseNotificationListener listener;

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
    @DisplayName("비활성 DM 이벤트 발생 시 수신자에게 direct-messages 이벤트를 전송한다")
    void onInactiveDirectMessage_delegatesToSseSender() {
        // given
        UUID receiverId = UUID.randomUUID();
        DirectMessageResponse message = dmMessage(receiverId);

        // when
        listener.onInactiveDirectMessage(new InactiveDirectMessageEvent(message));

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
        UserSummaryResponse sender = new UserSummaryResponse(UUID.randomUUID(), "A", null);
        UserSummaryResponse receiver = new UserSummaryResponse(receiverId, "B", null);
        return new DirectMessageResponse(
                UUID.randomUUID(), UUID.randomUUID(), sender, receiver, "안녕하세요", Instant.now());
    }
}
