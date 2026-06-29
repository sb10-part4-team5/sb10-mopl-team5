package com.codeit.team5.mopl.notification.eventlistener;

import com.codeit.team5.mopl.notification.dto.NotificationPayload;
import com.codeit.team5.mopl.notification.event.NotificationCreatedEvent;
import com.codeit.team5.mopl.sse.emitter.SseEmitterStore;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@RequiredArgsConstructor
@Slf4j
public class SseNotificationListener {

    private final SseEmitterStore emitterStore;

    // NotificationService에서 알림이 생성되고 알림 생성 이벤트가 발행될 때 수행
    @EventListener
    public void onNotificationCreated(NotificationCreatedEvent event) {
        // 전달받은 이벤트에서 payload 추출
        NotificationPayload payload = event.notificationPayload();
        // 페이로드 내의 수신자 ID 추출
        UUID receiverId = payload.receiverId();

        // 수신자의 SseEmitter를 가져옴
        SseEmitter emitter = emitterStore.get(receiverId);
        if (emitter == null) {
            log.debug("SSE emitter not found for receiverId={}, skipping", receiverId);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .id(payload.notificationId().toString())
                    .name("notification")
                    .data(payload));
            log.debug("SSE notification sent: receiverId={}, notificationId={}",
                    receiverId, payload.notificationId());
        } catch (Exception e) {
            log.warn("SSE send failed: receiverId={}", receiverId);
            emitterStore.remove(receiverId);
        }
    }
}
