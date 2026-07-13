package com.codeit.team5.mopl.sse.service;

import com.codeit.team5.mopl.notification.dto.NotificationPayload;
import com.codeit.team5.mopl.sse.emitter.SseEmitterStore;
import com.codeit.team5.mopl.sse.exception.InvalidLastEventIdException;
import com.codeit.team5.mopl.sse.provider.MissedNotificationProvider;
import com.codeit.team5.mopl.sse.sender.SseSender;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
@Slf4j
public class SseService {

    private static final long SSE_TIMEOUT = 60 * 60 * 1000L;

    private final SseEmitterStore emitterStore;
    private final MissedNotificationProvider missedNotificationProvider;
    private final SseSender sseSender;

    public SseEmitter subscribe(UUID userId, String lastEventId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT); // 지정된 TIMEOUT을 가진 SseEmitter 객체 생성

        // userId를 키로 Emitter 저장 시도
        // 해당 userId로 Emitter가 저장되어 있다면, 이미 존재하는 k-v 쌍을 가져옴
        SseEmitter previous = emitterStore.save(userId, emitter);
        if (previous != null) { // 이미 존재하는 userId-Emitter 쌍이 있다면
            previous.complete(); // 해당 emitter를 정리한다.
        }

        // emitter가 작업을 완료하면 클라이언트에게 종료 신호를 전송하고
        // emitterStore에서 해당 emitter 제거
        // emitter.complete() 가 이 콜백을 호출합니다.
        emitter.onCompletion(() -> {
            log.debug("SSE connection completed: userId={}", userId);
            emitterStore.remove(userId, emitter);
        });

        // emitter가 타임아웃을 초과하면 emitter 연결 끊고 저장소 정리
        emitter.onTimeout(() -> {
            log.debug("SSE connection timed out: userId={}", userId);
            emitter.complete();
        });

        // emitter가 작업 중 예외를 발생시키면 -> 클라이언트는 연결이 이미 끊김
        // emitterStore만 정리
        emitter.onError(e -> {
            log.debug("SSE connection error: userId={}", userId);
            emitterStore.remove(userId, emitter);
        });

        // sseSender를 통해 구독 event 전달
        if (!sseSender.send(userId, emitter, SseEmitter.event().name("connect").data("connected"))) {
            return emitter; // 전달 실패 시 emitter 반환
        }

        // lastEventId가 존재하면 미수신 알림이 존재
        if (lastEventId != null) {
            try {
                sendMissedEvents(emitter, userId, lastEventId); // 미수신 이벤트 전송
            } catch (InvalidLastEventIdException e) {
                log.warn("Invalid Last-Event-ID: userId={}, reason={}", userId, e.getMessage());
                emitterStore.remove(userId, emitter);
                emitter.complete();
                throw e;
            }
        }
        return emitter;
    }

    // 미수신 이벤트 전달
    private void sendMissedEvents(SseEmitter emitter, UUID userId, String lastEventId) {
        UUID lastEventIdUuid;

        // 마지막으로 수신 받았던 이벤트 ID가 형식에 어긋나면 예외 던짐
        try {
            lastEventIdUuid = UUID.fromString(lastEventId);
        } catch (IllegalArgumentException e) {
            throw new InvalidLastEventIdException(lastEventId);
        }

        // 미수신된 알림들을 가져옴
        List<NotificationPayload> notifications = missedNotificationProvider.findMissedNotifications(userId, lastEventIdUuid);

        // 미수신된 알림 페이로드들을 순회하면서 sseSender를 통해 send
        for (NotificationPayload payload : notifications) {
            SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .id(payload.eventId().toString())
                    .name("notifications")
                    .data(payload);
            if (!sseSender.send(userId, emitter, event)) {
                emitter.complete();
                return;
            }
        }

        log.debug("SSE 미수신 이벤트 전달: userId={}, notifications={}", userId, notifications.size());
    }
}
