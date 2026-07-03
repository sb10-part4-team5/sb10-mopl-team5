package com.codeit.team5.mopl.sse.eventlistener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.dm.event.InactiveDirectMessageEvent;
import com.codeit.team5.mopl.notification.dto.NotificationPayload;
import com.codeit.team5.mopl.notification.event.NotificationCreatedEvent;
import com.codeit.team5.mopl.sse.sender.SseSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@RequiredArgsConstructor
@Slf4j
public class SseNotificationListener {

    private final SseSender sseSender;

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        NotificationPayload payload = event.notificationPayload();
        sseSender.sendToUser(payload.receiverId(),
                SseEmitter.event()
                        .id(payload.notificationId().toString())
                        .name("notifications")
                        .data(payload));
    }

    // 비활성 대화에 DM이 도착하면 SSE "direct-messages" 이벤트로 전송 (알림 저장과 독립)
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void onInactiveDirectMessage(InactiveDirectMessageEvent event) {
        DirectMessageResponse message = event.message();
        sseSender.sendToUser(message.receiver().userId(),
                SseEmitter.event()
                        .id(message.id().toString())
                        .name("direct-messages")
                        .data(message));
    }
}
