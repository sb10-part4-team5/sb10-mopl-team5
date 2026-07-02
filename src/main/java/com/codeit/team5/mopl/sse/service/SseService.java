package com.codeit.team5.mopl.sse.service;

import com.codeit.team5.mopl.global.exception.BusinessException;
import com.codeit.team5.mopl.notification.dto.NotificationPayload;
import com.codeit.team5.mopl.notification.service.NotificationService;
import com.codeit.team5.mopl.sse.dto.DirectMessagePayload;
import com.codeit.team5.mopl.sse.emitter.SseEmitterStore;
import com.codeit.team5.mopl.sse.exception.InvalidLastEventIdException;
import com.codeit.team5.mopl.sse.exception.SseMissedEventSendFailException;
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
        // 구독 시 새로운 emitter를 생성
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 이전 emitter가 존재하는지 체크
        SseEmitter previous = emitterStore.save(userId, emitter);
        if (previous != null) {
            previous.complete(); // 있던 previous emitter는 정리
        }

        // emitter가 작업을 마무리하고 나면 정리
        emitter.onCompletion(() -> {
            log.debug("SSE connection completed: userId={}", userId);
            emitterStore.remove(userId, emitter);
        });
        // emitter가 timeout에 걸리면 정리
        emitter.onTimeout(() -> {
            log.debug("SSE connection timed out: userId={}", userId);
            emitter.complete();
        });

        // emitter가 작업 도중 에러를 발생하면 정리
        emitter.onError(e -> {
            log.debug("SSE connection error: userId={}", userId);
            emitterStore.remove(userId, emitter);
        });

        // emitter에 연결 이벤트를 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected"));
        } catch (Exception e) { // 예외 발생 시 emitter 정리 및 반환
            log.warn("SSE connect event send failed: userId={}", userId);
            emitterStore.remove(userId, emitter);
            return emitter;
        }

        // lastEventId 파라미터가 존재한다면 미수신 이벤트 존재.
        if (lastEventId != null) {
            try {
                sendMissedEvents(emitter, userId, lastEventId); // 미수신 이벤트를 전송하는 메서드 호출
            } catch (BusinessException e) {
                log.warn("SSE missed event recovery failed: userId={}, reason={}", userId, e.getMessage());
                emitterStore.remove(userId, emitter);
                emitter.completeWithError(e);
            }
        }

        return emitter;
    }

    // 미수신 이벤트 전송 메서드
    private void sendMissedEvents(SseEmitter emitter, UUID userId, String lastEventId) {
        UUID lastNotificationId;
        try {
            // 마지막으로 수신받은 알림이벤트 ID를 UUID로 파싱
            lastNotificationId = UUID.fromString(lastEventId);
        } catch (IllegalArgumentException e) { // 예외 발생 -> 커스텀 예외 변환
            log.warn("Invalid Last-Event-ID format: {}", lastEventId);
            throw new InvalidLastEventIdException(lastEventId);
        }

        // 미수신 알림 목록
        List<NotificationPayload> notifications =
                notificationService.findMissedNotifications(userId, lastNotificationId);
        // 미수신 DM 목록
        List<DirectMessagePayload> dms =
                notificationService.findMissedDirectMessages(userId, lastNotificationId);

        // 두 정렬 리스트를 (createdAt, id) 순서로 인터리빙하여 원래 스트림 순서 보존
        int ni = 0, di = 0;
        while (ni < notifications.size() || di < dms.size()) {
            boolean pickNotification;
            if (ni >= notifications.size()) {
                pickNotification = false;
            } else if (di >= dms.size()) {
                pickNotification = true;
            } else {
                NotificationPayload n = notifications.get(ni);
                DirectMessagePayload d = dms.get(di);
                int cmp = n.createdAt().compareTo(d.createdAt());
                pickNotification = cmp < 0 || (cmp == 0 && n.notificationId().compareTo(d.id()) < 0);
            }

            if (pickNotification) {
                NotificationPayload payload = notifications.get(ni++);
                try {
                    emitter.send(SseEmitter.event()
                            .id(payload.notificationId().toString())
                            .name("notifications")
                            .data(payload));
                } catch (Exception e) {
                    log.warn("SSE missed notification send failed: userId={}", userId);
                    emitterStore.remove(userId, emitter);
                    throw new SseMissedEventSendFailException();
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
                    throw new SseMissedEventSendFailException();
                }
            }
        }

        log.debug("SSE missed events sent: userId={}, notifications={}, dms={}",
                userId, notifications.size(), dms.size());
    }
}
