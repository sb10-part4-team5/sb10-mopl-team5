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

        // 이전 SseEmitter가 존재한다면 해당 emitter 정리
        SseEmitter previous = emitterStore.save(userId, emitter);
        // store 내부적으로 put 메소드 사용.
        // 이전값이 존재하면 previous != null
        // 이전값이 존재하지 않으면 previous == null
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

        // 발행된 emitter에 연결 이벤트를 송신함
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected"));
            // 송신 중 예외 발생 시 해당 emitter 정리 및 반환
        } catch (Exception e) {
            log.warn("SSE connect event send failed: userId={}", userId);
            emitterStore.remove(userId, emitter);
            return emitter;
        }

        // 마지막으로 받았던 이벤트 ID가 존재한다면
        if (lastEventId != null) {
            // 놓쳤던 알림을 전송하는 메서드 호출
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
    // TODO: 임시적으로 for문을 돌려서 하나하나 비교해가면서 순서를 지키고 있는중
    // TODO: 추후 Refactor 할 여지가 남아있다고 봄.
    private void sendMissedNotifications(SseEmitter emitter, UUID userId, String lastEventId) {
        UUID lastNotificationId;
        try {
            lastNotificationId = UUID.fromString(lastEventId);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid Last-Event-ID format: {}", lastEventId);
            throw new InvalidLastEventIdException(lastEventId);
        }

        List<NotificationPayload> notifications =
                notificationService.findMissedNotifications(userId, lastNotificationId);
        // TODO: DM 도메인 구현 후 DirectMessageDto로 교체
        List<DirectMessagePayload> dms =
                notificationService.findMissedDirectMessages(userId, lastNotificationId);

        // 두 정렬 리스트를 (createdAt, id) 순서로 인터리빙하여 원래 스트림 순서 보존
        int ni = 0, di = 0; // 두 리스트를 순회하기 위한 인덱스 (ni = notification index, di = dm index)
        while (ni < notifications.size() || di < dms.size()) { // 알림 목록이나 dm 목록 둘 중 아무 목록이든 순회가 다 끝나면 반복문 종료
            boolean pickNotification;
            if (ni >= notifications.size()) { // 알림 목록 순회 끝
                pickNotification = false; // 알림 잔재 X
            } else if (di >= dms.size()) { // dm 목록 순회 끝
                pickNotification = true; // 알림 잔재 O
            } else {
                // 두 목록 순회할 목록이 남아있으면 createdAt 비교
                NotificationPayload n = notifications.get(ni); // ni 번째에 해당하는 알림 페이로드 가져옴
                DirectMessagePayload d = dms.get(di); // di 번째에 해당하는 dm 페이로드 가져옴
                int cmp = n.createdAt().compareTo(d.createdAt()); // 알림과 dm의 생성일자를 비교
                pickNotification = cmp < 0 || (cmp == 0 && n.notificationId().compareTo(d.id()) < 0);
            }

            // 알림이 먼저왔으면 알림을 전송함
            if (pickNotification) {
                NotificationPayload payload = notifications.get(ni++); // ni가 가르키는 알림 페이로드를 가져오고 이후 ni를 1 증가
                // 해당 알림 sse 전송
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
                // DM 페이로드의 createdAt이 먼저면 dm 페이로드 sse 전송
            } else {
                DirectMessagePayload payload = dms.get(di++); // di가 가르키고 있는 dm 페이로드 추출 후 di 1 증가
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
