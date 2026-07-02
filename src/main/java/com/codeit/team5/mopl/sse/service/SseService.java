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

    private void sendMissedEvents(SseEmitter emitter, UUID userId, String lastEventId) {
        List<NotificationPayload> notifications = notificationService.findMissedNotifications(userId,UUID.fromString(lastEventId));
        List<DirectMessagePayload> dms = notificationService.findMissedDirectMessages(userId, UUID.fromString(lastEventId));

        Stream.concat(notifications.stream(), dms.stream())
            .sorted()
            .forEach(payload -> {
                SseEmitter.SseEventBuilder event;

                if(payload instanceof NotificationPayload notification) {
                    event = SseEmitter.event()
                        .id(notification.eventId().toString())
                        .name("notification")
                        .data(notification);
                } else if (payload instanceof DirectMessagePayload dm){
                    event = SseEmitter.event()
                        .id(dm.eventId().toString())
                        .name("direct-message")
                        .data(dm);
                } else {
                    return;
                }

                sseSender.send(userId, emitter, event);
            });

        log.debug("SSE 미수신 이벤트 전달: userId={}, notifications={}, dms={}",
                userId, notifications.size(), dms.size());
    }
}
