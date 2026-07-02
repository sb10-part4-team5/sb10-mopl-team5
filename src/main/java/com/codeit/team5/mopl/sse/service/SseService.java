package com.codeit.team5.mopl.sse.service;

import com.codeit.team5.mopl.notification.dto.NotificationPayload;
import com.codeit.team5.mopl.notification.service.NotificationService;
import com.codeit.team5.mopl.sse.dto.DirectMessagePayload;
import com.codeit.team5.mopl.sse.emitter.SseEmitterStore;
import com.codeit.team5.mopl.sse.exception.InvalidLastEventIdException;
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
    private final NotificationService notificationService;

    public SseEmitter subscribe(UUID userId, String lastEventId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 이전 emitter 여부 확인
        SseEmitter previous = emitterStore.save(userId, emitter);
        if (previous != null) {
            previous.complete(); // 존재 시 이전 emitter 정리
        }

        // emitter가 끝날 때
        emitter.onCompletion(() -> {
            log.debug("SSE connection completed: userId={}", userId);
            emitterStore.remove(userId, emitter); // emitterStore에서 삭제
        });
        // emitter가 타임아웃에 걸릴 때
        emitter.onTimeout(() -> {
            log.debug("SSE connection timed out: userId={}", userId);
            emitter.complete(); // emitter.complete으로 정리
        });
        // emitter가 에러 터졌을 때
        emitter.onError(e -> {
            log.debug("SSE connection error: userId={}", userId);
            emitterStore.remove(userId, emitter); // emitterStore에서 해당 emitter 삭제
        });

        // 연결 이벤트 송신
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected"));
        } catch (Exception e) {
            log.warn("SSE connect event send failed: userId={}", userId);
            emitterStore.remove(userId, emitter); // 예외 발생 시 emitterStore에서 삭제
            return emitter; // emitter 반환하여 정리
        }

        // 헤더에 lastEventId가 포함되어 있으면 미수신 이벤트 전송 private 메서드 호출
        if (lastEventId != null) {
            try {
                sendMissedEvents(emitter, userId, lastEventId);
            } catch (InvalidLastEventIdException e) {
                log.warn("Invalid Last-Event-ID: userId={}, reason={}", userId, e.getMessage());
                emitterStore.remove(userId, emitter);
                emitter.complete();
                throw e;
            }
        }
        return emitter;
    }

    private void sendMissedEvents(SseEmitter emitter, UUID userId, String lastEventId) {
        UUID lastNotificationId;
        try {
            lastNotificationId = UUID.fromString(lastEventId);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid Last-Event-ID format: {}", lastEventId);
            throw new InvalidLastEventIdException(lastEventId);
        }

        List<NotificationPayload> notifications =
                notificationService.findMissedNotifications(userId, lastNotificationId);
        List<DirectMessagePayload> dms =
                notificationService.findMissedDirectMessages(userId, lastNotificationId);

        // 두 정렬 리스트를 (createdAt, id) 순서로 인터리빙하여 원래 스트림 순서 보존
        int ni = 0, di = 0; // ni = 알림 인덱스, di = dm 인덱스
        while (ni < notifications.size() || di < dms.size()) {
            boolean pickNotification;
            if (ni >= notifications.size()) {
                pickNotification = false;
            } else if (di >= dms.size()) {
                pickNotification = true;
            }
            // 알림과 dm 목록 내에 item이 남아있으면 ni, di가 가르키는 항목을 비교하면서
            // createdAt이 빠른 항목에 따라 pickNotification의 값을 판단한다.
            else {
                NotificationPayload n = notifications.get(ni);
                DirectMessagePayload d = dms.get(di);
                int cmp = n.createdAt().compareTo(d.createdAt());
                pickNotification = cmp < 0 || (cmp == 0 && n.notificationId().compareTo(d.id()) < 0);
            }

            // 알림의 createdAt이 더 빠를 때
            if (pickNotification) {
                NotificationPayload payload = notifications.get(ni++); // 인덱스 하나 증가

                // 해당 알림 payload를 sseEmitter.send 한다.
                try {
                    emitter.send(SseEmitter.event()
                            .id(payload.notificationId().toString())
                            .name("notifications")
                            .data(payload));
                } catch (Exception e) {
                    // 전송 실패는 연결/IO 문제 → 예외 전파 없이 emitter 정리 후 클라이언트 재연결 유도
                    log.warn("SSE missed notification send failed: userId={}", userId);
                    emitterStore.remove(userId, emitter);
                    emitter.complete();
                    return;
                }
            } else {
                DirectMessagePayload payload = dms.get(di++);
                try {
                    emitter.send(SseEmitter.event()
                            .id(payload.id().toString())
                            .name("direct-messages")
                            .data(payload));
                } catch (Exception e) {
                    log.warn("SSE missed DM send failed: userId={}", userId);
                    emitterStore.remove(userId, emitter);
                    emitter.complete();
                    return;
                }
            }
        }

        log.debug("SSE 미수신 이벤트 전달: userId={}, notifications={}, dms={}",
                userId, notifications.size(), dms.size());
    }
}
