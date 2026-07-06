package com.codeit.team5.mopl.sse.service;

import com.codeit.team5.mopl.notification.dto.NotificationPayload;
import com.codeit.team5.mopl.notification.repository.NotificationRepository;
import com.codeit.team5.mopl.notification.service.NotificationService;
import com.codeit.team5.mopl.sse.dto.DirectMessagePayload;
import com.codeit.team5.mopl.sse.emitter.SseEmitterStore;
import com.codeit.team5.mopl.sse.exception.InvalidLastEventIdException;
import com.codeit.team5.mopl.sse.sender.SseSender;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class SseService {

    private static final long SSE_TIMEOUT = 60 * 60 * 1000L;

    private final SseEmitterStore emitterStore;
    private final NotificationService notificationService;
    private final SseSender sseSender;
    private final NotificationRepository notificationRepository;

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

        // sseSender를 통해 event 전달
        if (!sseSender.send(userId, emitter, SseEmitter.event().name("connect").data("connected"))) {
            return emitter; // 전달 실패 시 emitter 반환
        }

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

    // 미수신 이벤트 전달
    private void sendMissedEvents(SseEmitter emitter, UUID userId, String lastEventId) {
        UUID lastEventIdUuid;
        // 마지막으로 수신 받았던 이벤트 ID가 형식에 어긋나면 예외 던짐
        try {
            lastEventIdUuid = UUID.fromString(lastEventId);
        } catch (IllegalArgumentException e) {
            throw new InvalidLastEventIdException(lastEventId);
        }

        List<NotificationPayload> notifications = notificationService.findMissedNotifications(userId, lastEventIdUuid);
        List<DirectMessagePayload> dms = notificationService.findMissedDirectMessages(userId, lastEventIdUuid);

        // 알림/dm 페이로드를 concat한 뒤 payload가 순회
        for (var payload : Stream.concat(notifications.stream(), dms.stream()).sorted().toList()) {
            SseEmitter.SseEventBuilder event;

            // item이 알림 페이로드라면 알림 이벤트
            if (payload instanceof NotificationPayload notification) {
                event = SseEmitter.event()
                    .id(notification.eventId().toString())
                    .name("notifications")
                    .data(notification);
            }
            // item이 dm 페이로드라면 direct-message 이벤트
            else if (payload instanceof DirectMessagePayload dm) {
                event = SseEmitter.event()
                    .id(dm.eventId().toString())
                    .name("direct-message")
                    .data(dm);
            } else {
                continue;
            }

            // send가 실패하면 emitter 정리
            if (!sseSender.send(userId, emitter, event)) {
                emitter.complete();
                return;
            }
        }

        log.debug("SSE 미수신 이벤트 전달: userId={}, notifications={}, dms={}",
                userId, notifications.size(), dms.size());
    }
}
