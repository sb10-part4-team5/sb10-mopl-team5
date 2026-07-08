package com.codeit.team5.mopl.dm.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.dm.event.DirectMessageBroadcastEvent;
import com.codeit.team5.mopl.dm.event.DirectMessageNotificationEvent;
import com.codeit.team5.mopl.dm.event.DirectMessageSseEvent;
import com.codeit.team5.mopl.dm.provider.ActiveConversationChecker;
import com.codeit.team5.mopl.user.dto.response.UserSummaryResponse;

@ExtendWith(MockitoExtension.class)
class DmActiveNotificationListenerTest {

    @Mock
    private ActiveConversationChecker activeConversationChecker;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DmActiveNotificationListener listener;

    private DirectMessageBroadcastEvent event(UUID receiverId) {
        UserSummaryResponse sender = new UserSummaryResponse(UUID.randomUUID(), "A", null);
        UserSummaryResponse receiver = new UserSummaryResponse(receiverId, "B", null);
        DirectMessageResponse message = new DirectMessageResponse(
                UUID.randomUUID(), UUID.randomUUID(), sender, receiver, "hi", Instant.now());
        return new DirectMessageBroadcastEvent(message, receiverId);
    }

    @Test
    @DisplayName("수신자가 대화방 비활성이면 알림·SSE 이벤트 발행 성공")
    void inactiveReceiver_publishesEvent_success() {
        // given
        UUID receiverId = UUID.randomUUID();
        DirectMessageBroadcastEvent event = event(receiverId);
        UUID conversationId = event.message().conversationId();
        when(activeConversationChecker.isViewing(eq(conversationId), eq(receiverId))).thenReturn(false);

        // when
        listener.onDirectMessageBroadcast(event);

        // then
        verify(eventPublisher).publishEvent(any(DirectMessageNotificationEvent.class));
        verify(eventPublisher).publishEvent(any(DirectMessageSseEvent.class));
    }

    @Test
    @DisplayName("수신자가 대화방 활성이면 알림 미발행 성공")
    void activeReceiver_noNotification_success() {
        // given
        UUID receiverId = UUID.randomUUID();
        DirectMessageBroadcastEvent event = event(receiverId);
        UUID conversationId = event.message().conversationId();
        when(activeConversationChecker.isViewing(eq(conversationId), eq(receiverId))).thenReturn(true);

        // when
        listener.onDirectMessageBroadcast(event);

        // then
        verify(eventPublisher, never()).publishEvent(any());
    }
}
