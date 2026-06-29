package com.codeit.team5.mopl.sse.controller;

import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.notification.dto.NotificationPayload;
import com.codeit.team5.mopl.notification.entity.NotificationType;
import com.codeit.team5.mopl.notification.exception.InvalidLastEventIdException;
import com.codeit.team5.mopl.notification.exception.SseMissedEventSendFailException;
import com.codeit.team5.mopl.notification.service.NotificationService;
import com.codeit.team5.mopl.sse.controller.api.SseApi;
import com.codeit.team5.mopl.sse.emitter.SseEmitterStore;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
@Slf4j
public class SseController implements SseApi {

    // SSE 타임아웃 시간
    private static final long SSE_TIMEOUT = 60 * 60 * 1000L;

    private final SseEmitterStore emitterStore;
    private final NotificationService notificationService;

    @Override
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @AuthenticationPrincipal MoplUserDetails userDetails,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {

        // SecurityContextHolder에 있는 유저 인증 정보로부터 현재 로그인하고 있는 사용자의 id를 추출한다.
        UUID userId = userDetails.getId();
        log.info("SSE subscribe request: GET /api/sse");

        // SseEmitter 객체 생성
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        // 로그인 한 유저의 id를 바탕으로 emitter 객체를 스토리지에 저장함.
        emitterStore.save(userId, emitter);

        // Emitter 연결이 종료되면 객체 정리
        emitter.onCompletion(() -> {
            log.debug("SSE connection completed: userId={}", userId);
            emitterStore.remove(userId);
        });
        // 타임아웃이 지나면 객체 정리
        emitter.onTimeout(() -> {
            log.debug("SSE connection timed out: userId={}", userId);
            emitter.complete();
        });
        // 에러 발생 시 객체 정리
        emitter.onError(e -> {
            log.debug("SSE connection error: userId={}", userId);
            emitterStore.remove(userId);
        });

        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected"));

            if (lastEventId != null) {
                sendMissedNotifications(emitter, userId, lastEventId);
            }
        } catch (Exception e) {
            log.warn("SSE initial event send failed: userId={}", userId);
            emitterStore.remove(userId);
        }

        return emitter;
    }

    // 미수신된 알림을 다시 전송하는 private 메소드
    private void sendMissedNotifications(SseEmitter emitter, UUID userId, String lastEventId) {
        UUID lastNotificationId;
        // 받은 String 형식의 Last-Event-ID를 UUID로 파싱
        try {
            lastNotificationId = UUID.fromString(lastEventId);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid Last-Event-ID format: {}", lastEventId);
            throw new InvalidLastEventIdException();
        }

        // 연결이 끊긴 동안 왔었던 알림들을 추출
        List<NotificationPayload> missed = notificationService.findMissedNotifications(userId, lastNotificationId);
        for (NotificationPayload payload : missed) {
            try {
                // 이벤트가 DM이냐 아니냐에 따라 direct-messages / notifications 으로 이벤트 작명
                String eventName = payload.type() == NotificationType.DIRECT_MESSAGE
                        ? "direct-messages" : "notifications";
                emitter.send(SseEmitter.event()
                        .id(payload.notificationId().toString()) // payload의 이벤트 id
                        .name(eventName) // event 이름
                        // TODO : 추후 DM 도메인 작성되면 direct-messages 이벤트는 payload가 DirectMessageDto를 담아야 함.
                        .data(payload)); // payload
            } catch (Exception e) {
                log.warn("SSE missed notification send failed: userId={}", userId);
                emitterStore.remove(userId);
                throw new SseMissedEventSendFailException();
            }
        }
        log.debug("SSE missed notifications sent: userId={}, count={}", userId, missed.size());
    }
}
