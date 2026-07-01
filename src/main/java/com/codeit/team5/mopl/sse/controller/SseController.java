package com.codeit.team5.mopl.sse.controller;

import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.global.exception.BusinessException;
import com.codeit.team5.mopl.sse.dto.DirectMessagePayload;
import com.codeit.team5.mopl.notification.dto.NotificationPayload;
import com.codeit.team5.mopl.sse.exception.InvalidLastEventIdException;
import com.codeit.team5.mopl.sse.exception.SseMissedEventSendFailException;
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

        // 이전 SseEmitter
        SseEmitter previous = emitterStore.save(userId, emitter);
        if(previous != null){
            previous.complete();
        }

        // Emitter 연결이 종료되면 객체 정리
        emitter.onCompletion(() -> {
            log.debug("SSE connection completed: userId={}", userId);
            emitterStore.remove(userId, emitter);
        });
        // 타임아웃이 지나면 객체 정리
        emitter.onTimeout(() -> {
            log.debug("SSE connection timed out: userId={}", userId);
            emitter.complete();
        });
        // 에러 발생 시 객체 정리
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
                sendMissedNotifications(emitter, userId, lastEventId);
            } catch (BusinessException e) {
                // 도메인 예외(잘못된 Last-Event-ID, 재전송 실패)는 클라이언트에 알리고 연결 종료
                log.warn("SSE missed event recovery failed: userId={}, reason={}", userId, e.getMessage());
                emitterStore.remove(userId, emitter);
                emitter.completeWithError(e);
                return emitter;
            }
        }

        return emitter;
    }

    // 미수신된 이벤트를 다시 전송하는 private 메소드
    private void sendMissedNotifications(SseEmitter emitter, UUID userId, String lastEventId) {
        UUID lastNotificationId;
        try {
            lastNotificationId = UUID.fromString(lastEventId);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid Last-Event-ID format: {}", lastEventId);
            throw new InvalidLastEventIdException(lastEventId);
        }

        // 미수신 일반 알림 전송
        List<NotificationPayload> missedNotifications =
                notificationService.findMissedNotifications(userId, lastNotificationId);
        for (NotificationPayload payload : missedNotifications) {
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
        }

        // 미수신 DM 전송
        // TODO: DM 도메인 구현 후 DirectMessageDto로 교체
        List<DirectMessagePayload> missedDms =
                notificationService.findMissedDirectMessages(userId, lastNotificationId);
        for (DirectMessagePayload payload : missedDms) {
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

        log.debug("SSE missed events sent: userId={}, notifications={}, dms={}",
                userId, missedNotifications.size(), missedDms.size());
    }
}
