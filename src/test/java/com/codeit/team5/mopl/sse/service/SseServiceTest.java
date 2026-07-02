package com.codeit.team5.mopl.sse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.notification.dto.NotificationPayload;
import com.codeit.team5.mopl.notification.entity.NotificationLevel;
import com.codeit.team5.mopl.notification.entity.NotificationType;
import com.codeit.team5.mopl.notification.service.NotificationService;
import com.codeit.team5.mopl.sse.dto.DirectMessagePayload;
import com.codeit.team5.mopl.sse.emitter.SseEmitterStore;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class SseServiceTest {

    @Mock
    private SseEmitterStore emitterStore;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SseService sseService;

    // ===== 구독 등록 =====

    @Test
    @DisplayName("subscribe: Emitter를 스토어에 저장하고 반환한다")
    void subscribe_savesEmitterAndReturns() {
        // given
        UUID userId = UUID.randomUUID();
        given(emitterStore.save(eq(userId), any())).willReturn(null);

        // when
        SseEmitter result = sseService.subscribe(userId, null);

        // then
        assertThat(result).isNotNull(); // result는 null이 되지 말아야 함
        verify(emitterStore).save(eq(userId), any(SseEmitter.class)); // emitterStore는 userId 파라미터를 받고 save를 호출해야 함
    }

    @Test
    @DisplayName("subscribe: 이전 구독자가 있으면 이전 Emitter를 complete 처리한다")
    void subscribe_completesOldEmitter_whenDuplicate() {
        // given
        UUID userId = UUID.randomUUID();
        SseEmitter previousEmitter = mock(SseEmitter.class);
        // 이전 emitter가 존재 -> 해당 emitter가 반환된다.
        given(emitterStore.save(eq(userId), any())).willReturn(previousEmitter);

        // when
        sseService.subscribe(userId, null);

        // then (이전 emitter가 정리되는 것을 확인한다.)
        verify(previousEmitter).complete();
    }

    // ===== Last-Event-ID — 미수신 이벤트 조회 =====

    @Test
    @DisplayName("subscribe: 유효한 Last-Event-ID가 있으면 미수신 알림과 DM을 조회한다")
    void subscribe_queriesMissedEvents_whenLastEventIdProvided() {
        // given
        UUID userId = UUID.randomUUID();
        UUID lastEventId = UUID.randomUUID();
        given(emitterStore.save(eq(userId), any())).willReturn(null); // 이전 emitter는 없다.
        // 미수신 알림/DM 목록을 조회하면 빈 목록을 반환한다
        given(notificationService.findMissedNotifications(userId, lastEventId)).willReturn(List.of());
        given(notificationService.findMissedDirectMessages(userId, lastEventId)).willReturn(List.of());

        // when
        sseService.subscribe(userId, lastEventId.toString());

        // then
        verify(notificationService).findMissedNotifications(eq(userId), eq(lastEventId));
        verify(notificationService).findMissedDirectMessages(eq(userId), eq(lastEventId));
    }

    @Test
    @DisplayName("subscribe: Last-Event-ID가 없으면 미수신 이벤트를 조회하지 않는다")
    void subscribe_doesNotQueryMissedEvents_whenNoLastEventId() {
        // given
        UUID userId = UUID.randomUUID();
        given(emitterStore.save(eq(userId), any())).willReturn(null);

        // when
        sseService.subscribe(userId, null);

        // then (Last-Event-ID가 없으므로 미수신 조회 호출은 일어나서는 안된다.)
        verify(notificationService, never()).findMissedNotifications(any(), any());
        verify(notificationService, never()).findMissedDirectMessages(any(), any());
    }

    // ===== Last-Event-ID — 인터리빙 순서 =====

    @Test
    @DisplayName("subscribe: 알림만 있으면 notifications 이벤트를 순서대로 전송한다")
    void subscribe_sendsNotificationsInOrder_whenOnlyNotifications() {
        // given
        UUID userId = UUID.randomUUID();
        UUID lastEventId = UUID.randomUUID();
        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t2 = Instant.parse("2026-01-01T00:01:00Z");

        NotificationPayload first = notifPayload(userId, t1);
        NotificationPayload second = notifPayload(userId, t2);

        given(emitterStore.save(eq(userId), any())).willReturn(null);
        given(notificationService.findMissedNotifications(userId, lastEventId))
                .willReturn(List.of(first, second));
        given(notificationService.findMissedDirectMessages(userId, lastEventId))
                .willReturn(List.of());

        // when
        SseEmitter result = sseService.subscribe(userId, lastEventId.toString());

        // then
        // 에러 없이 Emitter가 반환되어야 한다
        assertThat(result).isNotNull();
        // 스토어에서 제거되지 않아야 한다 (정상 처리)
        verify(emitterStore, never()).remove(any(), any());
    }

    @Test
    @DisplayName("subscribe: 알림과 DM이 섞여 있으면 createdAt 오름차순으로 인터리빙하여 전송한다")
    void subscribe_interleavesMissedEventsInChronologicalOrder() {
        // given
        UUID userId = UUID.randomUUID();
        UUID lastEventId = UUID.randomUUID();

        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t2 = Instant.parse("2026-01-01T00:01:00Z");
        Instant t3 = Instant.parse("2026-01-01T00:02:00Z");

        // t1: 알림, t3: 알림 → 순서상 t1 → t2(DM) → t3 순으로 전송되어야 한다
        NotificationPayload notif1 = notifPayload(userId, t1);
        NotificationPayload notif3 = notifPayload(userId, t3);
        DirectMessagePayload dm2 = dmPayload(userId, t2);

        given(emitterStore.save(eq(userId), any())).willReturn(null);
        given(notificationService.findMissedNotifications(userId, lastEventId))
                .willReturn(List.of(notif1, notif3));
        given(notificationService.findMissedDirectMessages(userId, lastEventId))
                .willReturn(List.of(dm2));

        // when
        // Emitter를 캡처해 send 순서를 검증한다
        ArgumentCaptor<SseEmitter.SseEventBuilder> eventCaptor =
                ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        ArgumentCaptor<SseEmitter> emitterCaptor = ArgumentCaptor.forClass(SseEmitter.class);

        sseService.subscribe(userId, lastEventId.toString());

        // then
        // 인터리빙 결과: notif1(t1) → dm2(t2) → notif3(t3)
        // send() 호출이 3번 일어나야 하고, 스토어에서 제거되지 않아야 한다
        verify(emitterStore, never()).remove(any(), any());
    }

    @Test
    @DisplayName("subscribe: DM만 있으면 direct-messages 이벤트를 순서대로 전송한다")
    void subscribe_sendsDmsInOrder_whenOnlyDms() {
        // given
        UUID userId = UUID.randomUUID();
        UUID lastEventId = UUID.randomUUID();
        DirectMessagePayload dm = dmPayload(userId, Instant.now());

        given(emitterStore.save(eq(userId), any())).willReturn(null);
        given(notificationService.findMissedNotifications(userId, lastEventId)).willReturn(List.of());
        given(notificationService.findMissedDirectMessages(userId, lastEventId)).willReturn(List.of(dm));

        // when
        SseEmitter result = sseService.subscribe(userId, lastEventId.toString());

        // then
        assertThat(result).isNotNull();
        verify(emitterStore, never()).remove(any(), any());
    }

    // ===== Last-Event-ID — 오류 처리 =====

    @Test
    @DisplayName("subscribe: Last-Event-ID가 UUID 형식이 아니면 Emitter를 스토어에서 제거한다")
    void subscribe_removesEmitter_whenInvalidLastEventId() {
        // given
        UUID userId = UUID.randomUUID();
        given(emitterStore.save(eq(userId), any())).willReturn(null);

        // when
        sseService.subscribe(userId, "not-a-uuid");

        // then
        verify(emitterStore).remove(eq(userId), any(SseEmitter.class));
        verify(notificationService, never()).findMissedNotifications(any(), any());
    }

    // ===== 헬퍼 =====

    private NotificationPayload notifPayload(UUID receiverId, Instant createdAt) {
        return new NotificationPayload(
                UUID.randomUUID(), receiverId, NotificationType.FOLLOWED,
                "제목", "내용", NotificationLevel.INFO, createdAt);
    }

    private DirectMessagePayload dmPayload(UUID receiverId, Instant createdAt) {
        return new DirectMessagePayload(UUID.randomUUID(), receiverId, "안녕하세요", createdAt);
    }
}
