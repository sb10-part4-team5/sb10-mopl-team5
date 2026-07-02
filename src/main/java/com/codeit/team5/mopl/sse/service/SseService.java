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
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        SseEmitter previous = emitterStore.save(userId, emitter);
        if (previous != null) {
            previous.complete();
        }

        emitter.onCompletion(() -> {
            log.debug("SSE connection completed: userId={}", userId);
            emitterStore.remove(userId, emitter);
        });
        emitter.onTimeout(() -> {
            log.debug("SSE connection timed out: userId={}", userId);
            emitter.complete();
        });
        emitter.onError(e -> {
            log.debug("SSE connection error: userId={}", userId);
            emitterStore.remove(userId, emitter);
        });

        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected"));
        } catch (Exception e) {
            log.warn("SSE connect event send failed: userId={}", userId);
            emitterStore.remove(userId, emitter);
            return emitter;
        }

        if (lastEventId != null) {
            try {
                sendMissedEvents(emitter, userId, lastEventId);
            } catch (BusinessException e) {
                log.warn("SSE missed event recovery failed: userId={}, reason={}", userId, e.getMessage());
                emitterStore.remove(userId, emitter);
                emitter.completeWithError(e);
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
