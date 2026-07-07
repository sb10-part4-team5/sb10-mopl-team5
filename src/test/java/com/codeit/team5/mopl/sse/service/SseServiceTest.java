package com.codeit.team5.mopl.sse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.notification.dto.NotificationPayload;
import com.codeit.team5.mopl.notification.entity.NotificationLevel;
import com.codeit.team5.mopl.notification.entity.NotificationType;
import com.codeit.team5.mopl.notification.service.NotificationService;
import com.codeit.team5.mopl.sse.dto.DirectMessagePayload;
import com.codeit.team5.mopl.sse.emitter.SseEmitterStore;
import com.codeit.team5.mopl.sse.exception.InvalidLastEventIdException;
import com.codeit.team5.mopl.sse.sender.SseSender;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class SseServiceTest {

    @Mock
    private SseEmitterStore emitterStore;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SseSender sseSender;

    @InjectMocks
    private SseService sseService;

    // ===== 구독 등록 =====

    @Test
    @DisplayName("subscribe: Emitter를 스토어에 저장하고 반환한다")
    void subscribe_savesEmitterAndReturns() {
        // given
        UUID userId = UUID.randomUUID();
        given(emitterStore.save(eq(userId), any())).willReturn(null);
        given(sseSender.send(eq(userId), any(SseEmitter.class), any())).willReturn(true);

        // when
        SseEmitter result = sseService.subscribe(userId, null);

        // then
        assertThat(result).isNotNull();
        verify(emitterStore).save(eq(userId), any(SseEmitter.class));
        verify(sseSender).send(eq(userId), any(SseEmitter.class), any());
    }

    @Test
    @DisplayName("subscribe: 이전 구독자가 있으면 이전 Emitter를 complete 처리한다")
    void subscribe_completesOldEmitter_whenDuplicate() {
        // given
        UUID userId = UUID.randomUUID();
        SseEmitter previousEmitter = mock(SseEmitter.class);
        given(emitterStore.save(eq(userId), any())).willReturn(previousEmitter);
        given(sseSender.send(eq(userId), any(SseEmitter.class), any())).willReturn(true);

        // when
        sseService.subscribe(userId, null);

        // then
        verify(previousEmitter).complete();
    }

    @Test
    @DisplayName("subscribe: connect 이벤트 전송 실패 시 즉시 반환한다")
    void subscribe_returnsEarly_whenConnectSendFails() {
        // given
        UUID userId = UUID.randomUUID();
        given(emitterStore.save(eq(userId), any())).willReturn(null);
        given(sseSender.send(eq(userId), any(SseEmitter.class), any())).willReturn(false);

        // when
        SseEmitter result = sseService.subscribe(userId, null);

        // then
        assertThat(result).isNotNull();
        verify(notificationService, never()).findMissedNotifications(any(), any());
    }

    // ===== Last-Event-ID — 미수신 이벤트 조회 =====

    @Test
    @DisplayName("subscribe: 유효한 Last-Event-ID가 있으면 미수신 알림과 DM을 조회한다")
    void subscribe_queriesMissedEvents_whenLastEventIdProvided() {
        // given
        UUID userId = UUID.randomUUID();
        UUID lastEventId = UUID.randomUUID();
        given(emitterStore.save(eq(userId), any())).willReturn(null);
        given(sseSender.send(eq(userId), any(SseEmitter.class), any())).willReturn(true);
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
        given(sseSender.send(eq(userId), any(SseEmitter.class), any())).willReturn(true);

        // when
        sseService.subscribe(userId, null);

        // then
        verify(notificationService, never()).findMissedNotifications(any(), any());
        verify(notificationService, never()).findMissedDirectMessages(any(), any());
    }

    // ===== Last-Event-ID — 인터리빙 순서 =====

    @Test
    @DisplayName("subscribe: 알림만 있으면 notifications 이벤트를 createdAt 오름차순으로 전송한다")
    void subscribe_sendsNotificationsInOrder_whenOnlyNotifications() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID lastEventId = UUID.randomUUID();
        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t2 = Instant.parse("2026-01-01T00:01:00Z");

        NotificationPayload first = notifPayload(userId, t1);
        NotificationPayload second = notifPayload(userId, t2);

        given(emitterStore.save(eq(userId), any())).willReturn(null);
        given(sseSender.send(eq(userId), any(SseEmitter.class), any())).willReturn(true);
        given(notificationService.findMissedNotifications(userId, lastEventId))
                .willReturn(List.of(first, second));
        given(notificationService.findMissedDirectMessages(userId, lastEventId))
                .willReturn(List.of());

        // when
        sseService.subscribe(userId, lastEventId.toString());

        // then: connect(1) + notif(2) = 총 3번 send
        ArgumentCaptor<SseEmitter.SseEventBuilder> captor =
                ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(sseSender, times(3)).send(eq(userId), any(SseEmitter.class), captor.capture());

        List<SseEmitter.SseEventBuilder> sends = captor.getAllValues();
        // index 0 = connect, 1 = first(t1), 2 = second(t2) 순서여야 한다
        assertThat(extractPayload(sends.get(1), NotificationPayload.class))
                .map(NotificationPayload::id)
                .contains(first.id());
        assertThat(extractPayload(sends.get(2), NotificationPayload.class))
                .map(NotificationPayload::id)
                .contains(second.id());
    }

    @Test
    @DisplayName("subscribe: 알림과 DM이 섞여 있으면 createdAt 오름차순으로 인터리빙하여 전송한다")
    void subscribe_interleavesMissedEventsInChronologicalOrder() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID lastEventId = UUID.randomUUID();
        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t2 = Instant.parse("2026-01-01T00:01:00Z");
        Instant t3 = Instant.parse("2026-01-01T00:02:00Z");

        NotificationPayload notif1 = notifPayload(userId, t1);
        NotificationPayload notif3 = notifPayload(userId, t3);
        DirectMessagePayload dm2 = dmPayload(userId, t2);

        given(emitterStore.save(eq(userId), any())).willReturn(null);
        given(sseSender.send(eq(userId), any(SseEmitter.class), any())).willReturn(true);
        given(notificationService.findMissedNotifications(userId, lastEventId))
                .willReturn(List.of(notif1, notif3));
        given(notificationService.findMissedDirectMessages(userId, lastEventId))
                .willReturn(List.of(dm2));

        // when
        sseService.subscribe(userId, lastEventId.toString());

        // then: connect(1) + notif1(t1) + dm2(t2) + notif3(t3) = 총 4번 send
        ArgumentCaptor<SseEmitter.SseEventBuilder> captor =
                ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(sseSender, times(4)).send(eq(userId), any(SseEmitter.class), captor.capture());

        List<SseEmitter.SseEventBuilder> sends = captor.getAllValues();
        // index 0 = connect, 1 = notif1(t1), 2 = dm2(t2), 3 = notif3(t3) 순서여야 한다
        assertThat(extractPayload(sends.get(1), NotificationPayload.class))
                .map(NotificationPayload::id)
                .contains(notif1.id());
        assertThat(extractPayload(sends.get(2), DirectMessagePayload.class))
                .map(DirectMessagePayload::id)
                .contains(dm2.id());
        assertThat(extractPayload(sends.get(3), NotificationPayload.class))
                .map(NotificationPayload::id)
                .contains(notif3.id());
    }

    @Test
    @DisplayName("subscribe: DM만 있으면 direct-messages 이벤트를 전송한다")
    void subscribe_sendsDmsInOrder_whenOnlyDms() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID lastEventId = UUID.randomUUID();
        DirectMessagePayload dm = dmPayload(userId, Instant.now());

        given(emitterStore.save(eq(userId), any())).willReturn(null);
        given(sseSender.send(eq(userId), any(SseEmitter.class), any())).willReturn(true);
        given(notificationService.findMissedNotifications(userId, lastEventId)).willReturn(List.of());
        given(notificationService.findMissedDirectMessages(userId, lastEventId)).willReturn(List.of(dm));

        // when
        sseService.subscribe(userId, lastEventId.toString());

        // then: connect(1) + dm(1) = 총 2번 send
        ArgumentCaptor<SseEmitter.SseEventBuilder> captor =
                ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(sseSender, times(2)).send(eq(userId), any(SseEmitter.class), captor.capture());

        List<SseEmitter.SseEventBuilder> sends = captor.getAllValues();
        assertThat(extractPayload(sends.get(1), DirectMessagePayload.class))
                .map(DirectMessagePayload::id)
                .contains(dm.id());
    }

    // ===== Last-Event-ID — 오류 처리 =====

    @Test
    @DisplayName("subscribe: Last-Event-ID가 UUID 형식이 아니면 Emitter를 정리하고 InvalidLastEventIdException을 던진다")
    void subscribe_completesAndThrows_whenInvalidLastEventId() {
        // given
        UUID userId = UUID.randomUUID();
        given(emitterStore.save(eq(userId), any())).willReturn(null);
        given(sseSender.send(eq(userId), any(SseEmitter.class), any())).willReturn(true);

        // when + then
        try (MockedConstruction<SseEmitter> mocked = Mockito.mockConstruction(SseEmitter.class)) {
            assertThatThrownBy(() -> sseService.subscribe(userId, "not-a-uuid"))
                    .isInstanceOf(InvalidLastEventIdException.class);

            verify(mocked.constructed().get(0)).complete();
        }

        verify(emitterStore).remove(eq(userId), any(SseEmitter.class));
        verify(notificationService, never()).findMissedNotifications(any(), any());
    }

    // ===== 미수신 이벤트 전송 실패 (내부 처리) =====

    @Test
    @DisplayName("subscribe: 미수신 알림 전송 중 실패 시 예외 전파 없이 Emitter를 정리하고 연결을 닫는다")
    void subscribe_completesEmitterSilently_whenMissedNotificationSendFails() {
        // given
        UUID userId = UUID.randomUUID();
        UUID lastEventId = UUID.randomUUID();
        NotificationPayload missed = notifPayload(userId, Instant.now());

        given(emitterStore.save(eq(userId), any())).willReturn(null);
        given(notificationService.findMissedNotifications(userId, lastEventId))
                .willReturn(List.of(missed));
        given(notificationService.findMissedDirectMessages(userId, lastEventId))
                .willReturn(List.of());
        // connect → 성공, 미수신 알림 → 실패
        given(sseSender.send(eq(userId), any(SseEmitter.class), any()))
                .willReturn(true, false);

        // when
        try (MockedConstruction<SseEmitter> mocked = Mockito.mockConstruction(SseEmitter.class)) {
            SseEmitter result = sseService.subscribe(userId, lastEventId.toString());

            // then
            assertThat(result).isNotNull();
            verify(mocked.constructed().get(0)).complete();
        }
    }

    @Test
    @DisplayName("subscribe: 미수신 DM 전송 중 실패 시 예외 전파 없이 Emitter를 정리하고 연결을 닫는다")
    void subscribe_completesEmitterSilently_whenMissedDmSendFails() {
        // given
        UUID userId = UUID.randomUUID();
        UUID lastEventId = UUID.randomUUID();
        DirectMessagePayload missedDm = dmPayload(userId, Instant.now());

        given(emitterStore.save(eq(userId), any())).willReturn(null);
        given(notificationService.findMissedNotifications(userId, lastEventId))
                .willReturn(List.of());
        given(notificationService.findMissedDirectMessages(userId, lastEventId))
                .willReturn(List.of(missedDm));
        // connect → 성공, 미수신 DM → 실패
        given(sseSender.send(eq(userId), any(SseEmitter.class), any()))
                .willReturn(true, false);

        // when
        try (MockedConstruction<SseEmitter> mocked = Mockito.mockConstruction(SseEmitter.class)) {
            SseEmitter result = sseService.subscribe(userId, lastEventId.toString());

            // then
            assertThat(result).isNotNull();
            verify(mocked.constructed().get(0)).complete();
        }
    }

    // ===== 헬퍼 =====

    /**
     * SseEventBuilder.build()에서 특정 타입의 페이로드를 추출한다.
     * SSE 메타데이터(event:, id: 등)는 String으로 직렬화되므로 타입 필터로 구분한다.
     */
    private <T> Optional<T> extractPayload(SseEmitter.SseEventBuilder builder, Class<T> type) {
        return builder.build().stream()
                .map(ResponseBodyEmitter.DataWithMediaType::getData)
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst();
    }

    private NotificationPayload notifPayload(UUID receiverId, Instant createdAt) {
        return new NotificationPayload(
                UUID.randomUUID(), receiverId, NotificationType.FOLLOWED,
                "제목", "내용", NotificationLevel.INFO, createdAt);
    }

    private DirectMessagePayload dmPayload(UUID receiverId, Instant createdAt) {
        return new DirectMessagePayload(UUID.randomUUID(), receiverId, "안녕하세요", createdAt);
    }
}
