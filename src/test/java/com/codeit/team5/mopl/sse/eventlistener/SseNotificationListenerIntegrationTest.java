package com.codeit.team5.mopl.sse.eventlistener;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.notification.dto.NotificationPayload;
import com.codeit.team5.mopl.notification.entity.NotificationLevel;
import com.codeit.team5.mopl.notification.entity.NotificationType;
import com.codeit.team5.mopl.notification.event.DirectMessageCreatedEvent;
import com.codeit.team5.mopl.notification.event.NotificationCreatedEvent;
import com.codeit.team5.mopl.sse.dto.DirectMessagePayload;
import com.codeit.team5.mopl.sse.emitter.SseEmitterStore;
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

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class SseNotificationListenerIntegrationTest {

    @Autowired
    private SseEmitterStore emitterStore;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private TransactionTemplate tx;

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

        verify(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));
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

        verify(mockEmitter, never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("수신자의 Emitter가 없어도 예외 없이 조용히 무시한다")
    void onNotificationCreated_ignoresSilently_whenNoEmitter() {
        UUID receiverId = UUID.randomUUID(); // 등록된 emitter 없음

        assertThatNoException().isThrownBy(() ->
                tx.executeWithoutResult(status ->
                        publisher.publishEvent(new NotificationCreatedEvent(notificationPayload(receiverId)))));
    }

    // ===== DirectMessageCreatedEvent =====

    @Test
    @DisplayName("트랜잭션 커밋 후 DirectMessageCreatedEvent → 등록된 Emitter에 이벤트를 전송한다")
    void onDirectMessageCreated_sendsEvent_afterCommit() throws Exception {
        UUID receiverId = UUID.randomUUID();
        SseEmitter mockEmitter = mock(SseEmitter.class);
        emitterStore.save(receiverId, mockEmitter);

        DirectMessagePayload payload = new DirectMessagePayload(
                UUID.randomUUID(), receiverId, "안녕하세요", Instant.now());
        tx.executeWithoutResult(status ->
                publisher.publishEvent(new DirectMessageCreatedEvent(payload)));

        verify(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("트랜잭션 롤백 시 DirectMessageCreatedEvent → Emitter에 이벤트를 전송하지 않는다")
    void onDirectMessageCreated_doesNotSend_whenRollback() throws Exception {
        UUID receiverId = UUID.randomUUID();
        SseEmitter mockEmitter = mock(SseEmitter.class);
        emitterStore.save(receiverId, mockEmitter);

        DirectMessagePayload payload = new DirectMessagePayload(
                UUID.randomUUID(), receiverId, "안녕하세요", Instant.now());
        tx.executeWithoutResult(status -> {
            publisher.publishEvent(new DirectMessageCreatedEvent(payload));
            status.setRollbackOnly();
        });

        verify(mockEmitter, never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    // ===== 헬퍼 =====

    private NotificationPayload notificationPayload(UUID receiverId) {
        return new NotificationPayload(
                UUID.randomUUID(), receiverId, NotificationType.FOLLOWED,
                "팔로우 알림", "내용", NotificationLevel.INFO, Instant.now());
    }
}
