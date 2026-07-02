package com.codeit.team5.mopl.sse.eventlistener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.notification.dto.NotificationPayload;
import com.codeit.team5.mopl.notification.entity.NotificationLevel;
import com.codeit.team5.mopl.notification.entity.NotificationType;
import com.codeit.team5.mopl.notification.event.DirectMessageCreatedEvent;
import com.codeit.team5.mopl.notification.event.NotificationCreatedEvent;
import com.codeit.team5.mopl.sse.dto.DirectMessagePayload;
import com.codeit.team5.mopl.sse.emitter.SseEmitterStore;
import java.io.IOException;
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
    private SseEmitterStore emitterStore;

    @InjectMocks
    private SseNotificationListener listener;

    // ===== onNotificationCreated =====

    @Test
    @DisplayName("수신자의 Emitter가 존재하면 notifications 이벤트를 전송한다")
    void onNotificationCreated_sendsEvent_whenEmitterFound() throws Exception {
        // given
        UUID receiverId = UUID.randomUUID();
        NotificationPayload payload = notificationPayload(receiverId);
        SseEmitter mockEmitter = mock(SseEmitter.class);
        // emitterStore의 get 호출 시 mocking 해놓은 mockEmitter를 반환하게끔
        given(emitterStore.get(receiverId)).willReturn(mockEmitter);

        // when
        // 알림 이벤트 생성
        listener.onNotificationCreated(new NotificationCreatedEvent(payload));

        // then
        // 알림 이벤트 생성 시 sseEmitter는 send를 보내야 함
        verify(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("수신자의 Emitter가 없으면 이벤트 전송을 건너뛴다")
    void onNotificationCreated_skips_whenNoEmitter() {
        // given
        UUID receiverId = UUID.randomUUID();
        // 수신자가 구독 중이 아니어서 Emitter 존재 X
        given(emitterStore.get(receiverId)).willReturn(null);

        // when
        listener.onNotificationCreated(new NotificationCreatedEvent(notificationPayload(receiverId)));

        // then
        // 전송을 시도하지 않으므로 emitter 제거도 발생하지 않음
        verify(emitterStore, never()).remove(any(), any());
    }

    @Test
    @DisplayName("notifications 이벤트 전송 실패 시 Emitter를 스토어에서 제거한다")
    void onNotificationCreated_removesEmitter_whenSendFails() throws Exception {
        // given
        UUID receiverId = UUID.randomUUID();
        SseEmitter mockEmitter = mock(SseEmitter.class);
        given(emitterStore.get(receiverId)).willReturn(mockEmitter);
        // emitter 호출 시 IO예외 던지기
        doThrow(new IOException("broken pipe")).when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

        // when
        listener.onNotificationCreated(new NotificationCreatedEvent(notificationPayload(receiverId)));

        // then
        // 예외 발생 시 emitter가 제거되었는지 확인
        verify(emitterStore).remove(eq(receiverId), eq(mockEmitter));
    }

    // ===== onDirectMessageCreated =====

    @Test
    @DisplayName("수신자의 Emitter가 존재하면 direct-messages 이벤트를 전송한다")
    void onDirectMessageCreated_sendsEvent_whenEmitterFound() throws Exception {
        // given
        UUID receiverId = UUID.randomUUID();
        DirectMessagePayload payload = dmPayload(receiverId);
        SseEmitter mockEmitter = mock(SseEmitter.class);
        given(emitterStore.get(receiverId)).willReturn(mockEmitter);

        // when
        listener.onDirectMessageCreated(new DirectMessageCreatedEvent(payload));

        // then
        verify(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("수신자의 Emitter가 없으면 DM 이벤트 전송을 건너뛴다")
    void onDirectMessageCreated_skips_whenNoEmitter() {
        // given
        UUID receiverId = UUID.randomUUID();
        given(emitterStore.get(receiverId)).willReturn(null); // emitter X

        // when
        listener.onDirectMessageCreated(new DirectMessageCreatedEvent(dmPayload(receiverId)));

        // then
        // emitter 자체가 없으므로 정리도 일어나지 않는다.
        verify(emitterStore, never()).remove(any(), any());
    }

    @Test
    @DisplayName("DM 이벤트 전송 실패 시 Emitter를 스토어에서 제거한다")
    void onDirectMessageCreated_removesEmitter_whenSendFails() throws Exception {
        // given
        UUID receiverId = UUID.randomUUID();
        SseEmitter mockEmitter = mock(SseEmitter.class);
        given(emitterStore.get(receiverId)).willReturn(mockEmitter);
        // 전송 중 예외 발생
        doThrow(new IOException("broken pipe")).when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

        // when
        listener.onDirectMessageCreated(new DirectMessageCreatedEvent(dmPayload(receiverId)));

        // then
        verify(emitterStore).remove(eq(receiverId), eq(mockEmitter));
    }

    // ===== 헬퍼 =====

    // 수신자 ID를 받아 NotificationPayload를 생성한다.
    private NotificationPayload notificationPayload(UUID receiverId) {
        return new NotificationPayload(
                UUID.randomUUID(), receiverId, NotificationType.FOLLOWED,
                "제목", "내용", NotificationLevel.INFO, Instant.now());
    }

    // 수신자 ID를 받아 DirectMessagePayload 생성한다.
    private DirectMessagePayload dmPayload(UUID receiverId) {
        return new DirectMessagePayload(UUID.randomUUID(), receiverId, "안녕하세요", Instant.now());
    }
}
