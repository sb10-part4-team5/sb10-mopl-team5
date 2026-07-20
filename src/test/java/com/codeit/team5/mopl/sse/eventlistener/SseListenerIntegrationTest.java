package com.codeit.team5.mopl.sse.eventlistener;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.dm.event.DirectMessageBroadcastEvent;
import com.codeit.team5.mopl.dm.event.DirectMessageSseEvent;
import com.codeit.team5.mopl.dm.fixture.DirectMessageTestFixtures;
import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import com.codeit.team5.mopl.notification.dto.NotificationPayload;
import com.codeit.team5.mopl.notification.entity.NotificationLevel;
import com.codeit.team5.mopl.notification.entity.NotificationType;
import com.codeit.team5.mopl.notification.event.NotificationCreatedEvent;
import com.codeit.team5.mopl.sse.emitter.SseEmitterStore;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class SseListenerIntegrationTest {

    @Autowired
    private SseEmitterStore emitterStore;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private TransactionTemplate tx;

    @Autowired
    private WebSocketSessionStore webSocketSessionStore;

    // ===== NotificationCreatedEvent =====

    @Test
    @DisplayName("트랜잭션 커밋 후 NotificationCreatedEvent → 등록된 Emitter에 이벤트를 전송한다")
    void onNotificationCreated_sendsEvent_afterCommit() throws Exception {
        // given
        UUID receiverId = UUID.randomUUID();
        SseEmitter mockEmitter = mock(SseEmitter.class);
        emitterStore.save(receiverId, mockEmitter);

        NotificationPayload payload = notificationPayload(receiverId);
        tx.executeWithoutResult(status ->
                publisher.publishEvent(new NotificationCreatedEvent(payload)));

        // @Externalized → Kafka → @KafkaListener 비동기 흐름이므로 timeout으로 대기
        verify(mockEmitter, timeout(5000)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("트랜잭션 롤백 시 NotificationCreatedEvent → Emitter에 이벤트를 전송하지 않는다")
    void onNotificationCreated_doesNotSend_whenRollback() throws Exception {
        UUID receiverId = UUID.randomUUID();
        SseEmitter mockEmitter = mock(SseEmitter.class);
        emitterStore.save(receiverId, mockEmitter);

        NotificationPayload payload = notificationPayload(receiverId);
        tx.executeWithoutResult(status -> {
            publisher.publishEvent(new NotificationCreatedEvent(payload));
            status.setRollbackOnly();
        });

        // 롤백 시 Modulith가 Kafka에 발행하지 않으므로 일정 시간 후에도 미전송이어야 함
        verify(mockEmitter, after(500).never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("수신자의 Emitter가 없어도 예외 없이 조용히 무시한다")
    void onNotificationCreated_ignoresSilently_whenNoEmitter() {
        UUID receiverId = UUID.randomUUID(); // 등록된 emitter 없음

        assertThatNoException().isThrownBy(() ->
                tx.executeWithoutResult(status ->
                        publisher.publishEvent(new NotificationCreatedEvent(notificationPayload(receiverId)))));
    }

    // ===== 비활성 DM SSE =====

    @Test
    @DisplayName("비활성 DM SSE 이벤트 수신 시 Emitter 전송 성공")
    void onDirectMessageSse_sendsEvent_success() throws Exception {
        UUID receiverId = UUID.randomUUID();
        SseEmitter mockEmitter = mock(SseEmitter.class);
        emitterStore.save(receiverId, mockEmitter);

        publisher.publishEvent(new DirectMessageSseEvent(dmMessage(receiverId)));

        verify(mockEmitter, timeout(2000)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("트랜잭션 커밋 후 비활성 수신자에게 DM SSE 전송 성공")
    void directMessageBroadcast_commit_sendsToInactiveReceiver_success() throws Exception {
        UUID receiverId = UUID.randomUUID();
        SseEmitter mockEmitter = mock(SseEmitter.class);
        emitterStore.save(receiverId, mockEmitter);

        DirectMessageResponse message = dmMessage(receiverId);
        tx.executeWithoutResult(status ->
                publisher.publishEvent(new DirectMessageBroadcastEvent(message, receiverId)));

        verify(mockEmitter, timeout(2000)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("수신자가 대화방 활성이면 DM SSE 미전송 성공")
    void directMessageBroadcast_commit_doesNotSend_whenReceiverActive_success() throws Exception {
        UUID receiverId = UUID.randomUUID();
        SseEmitter mockEmitter = mock(SseEmitter.class);
        emitterStore.save(receiverId, mockEmitter);

        DirectMessageResponse message = dmMessage(receiverId);
        webSocketSessionStore.subscribe(receiverId, "sub-1", new WebSocketSessionStore.StompDestination(StompConstants.SUB_CONVERSATION_DM, message.conversationId()));

        tx.executeWithoutResult(status ->
                publisher.publishEvent(new DirectMessageBroadcastEvent(message, receiverId)));

        verify(mockEmitter, after(500).never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("트랜잭션 롤백 시 비활성 DM 미전송 성공")
    void directMessageBroadcast_rollback_doesNotSend_success() throws Exception {
        UUID receiverId = UUID.randomUUID();
        SseEmitter mockEmitter = mock(SseEmitter.class);
        emitterStore.save(receiverId, mockEmitter);

        DirectMessageResponse message = dmMessage(receiverId);
        tx.executeWithoutResult(status -> {
            publisher.publishEvent(new DirectMessageBroadcastEvent(message, receiverId));
            status.setRollbackOnly();
        });

        verify(mockEmitter, after(500).never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    // ===== 헬퍼 =====

    private DirectMessageResponse dmMessage(UUID receiverId) {
        return DirectMessageTestFixtures.dmMessage(receiverId);
    }

    private NotificationPayload notificationPayload(UUID receiverId) {
        return new NotificationPayload(
                UUID.randomUUID(), receiverId, NotificationType.FOLLOWED,
                "팔로우 알림", "내용", NotificationLevel.INFO, Instant.now());
    }
}
