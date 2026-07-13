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
import com.codeit.team5.mopl.sse.emitter.SseEmitterStore;
import com.codeit.team5.mopl.sse.exception.InvalidLastEventIdException;
import com.codeit.team5.mopl.sse.provider.MissedNotificationProvider;
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
    private MissedNotificationProvider missedNotificationProvider;

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
        verify(missedNotificationProvider, never()).findMissedNotifications(any(), any());
    }

    // ===== Last-Event-ID — 미수신 이벤트 조회 =====

    @Test
    @DisplayName("subscribe: 유효한 Last-Event-ID가 있으면 미수신 알림을 조회한다")
    void subscribe_queriesMissedEvents_whenLastEventIdProvided() {
        // given
        UUID userId = UUID.randomUUID();
        UUID lastEventId = UUID.randomUUID();
        given(emitterStore.save(eq(userId), any())).willReturn(null);
        given(sseSender.send(eq(userId), any(SseEmitter.class), any())).willReturn(true);
        given(missedNotificationProvider.findMissedNotifications(userId, lastEventId)).willReturn(List.of());

        // when
        sseService.subscribe(userId, lastEventId.toString());

        // then
        verify(missedNotificationProvider).findMissedNotifications(eq(userId), eq(lastEventId));
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
        verify(missedNotificationProvider, never()).findMissedNotifications(any(), any());
    }

    // ===== Last-Event-ID — 순서 =====

    @Test
    @DisplayName("subscribe: 미수신 알림을 createdAt 오름차순으로 전송한다")
    void subscribe_sendsNotificationsInOrder() {
        // given
        UUID userId = UUID.randomUUID();
        UUID lastEventId = UUID.randomUUID();
        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t2 = Instant.parse("2026-01-01T00:01:00Z");

        NotificationPayload first = notifPayload(userId, t1);
        NotificationPayload second = notifPayload(userId, t2);

        given(emitterStore.save(eq(userId), any())).willReturn(null);
        given(sseSender.send(eq(userId), any(SseEmitter.class), any())).willReturn(true);
        given(missedNotificationProvider.findMissedNotifications(userId, lastEventId))
                .willReturn(List.of(first, second));

        // when
        sseService.subscribe(userId, lastEventId.toString());

        // then: connect(1) + notif(2) = 총 3번 send
        ArgumentCaptor<SseEmitter.SseEventBuilder> captor =
                ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(sseSender, times(3)).send(eq(userId), any(SseEmitter.class), captor.capture());

        List<SseEmitter.SseEventBuilder> sends = captor.getAllValues();
        assertThat(extractPayload(sends.get(1), NotificationPayload.class))
                .map(NotificationPayload::id)
                .contains(first.id());
        assertThat(extractPayload(sends.get(2), NotificationPayload.class))
                .map(NotificationPayload::id)
                .contains(second.id());
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
        verify(missedNotificationProvider, never()).findMissedNotifications(any(), any());
    }

    @Test
    @DisplayName("subscribe: 미수신 알림 전송 중 실패 시 예외 전파 없이 Emitter를 정리하고 연결을 닫는다")
    void subscribe_completesEmitterSilently_whenMissedNotificationSendFails() {
        // given
        UUID userId = UUID.randomUUID();
        UUID lastEventId = UUID.randomUUID();
        NotificationPayload missed = notifPayload(userId, Instant.now());

        given(emitterStore.save(eq(userId), any())).willReturn(null);
        given(missedNotificationProvider.findMissedNotifications(userId, lastEventId))
                .willReturn(List.of(missed));
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

    // ===== 헬퍼 =====

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
}
