package com.codeit.team5.mopl.sse.eventlistener;

import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.dm.event.DirectMessageSseEvent;
import com.codeit.team5.mopl.global.infra.kafka.topic.KafkaTopics;
import com.codeit.team5.mopl.notification.dto.NotificationPayload;
import com.codeit.team5.mopl.notification.event.NotificationCreatedEvent;
import com.codeit.team5.mopl.sse.sender.SseSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@RequiredArgsConstructor
@Slf4j
public class SseListener {

    private final SseSender sseSender;

    // Kafka의 NOTIFICATION_SSE 토픽을 구독하는 리스너
    // 각 인스턴스가 고유한 groupId를 사용하므로 모든 인스턴스가 동일한 이벤트를 consume한다.
    // Spring Modulith @Externalized는 NotificationCreatedEvent 전체를 JSON 바이트로 직렬화한다.
    // ByteArrayDeserializer(yml 기본값)로 byte[]를 받고, JsonMessageConverter가 메서드
    // 파라미터 타입(NotificationCreatedEvent)을 추론해 역직렬화한다.
    // JsonDeserializer를 properties로 명시하면 JsonMessageConverter와 이중 변환 충돌 발생.
    @KafkaListener(
            topics = KafkaTopics.NOTIFICATION_SSE,
            groupId = "sse-${spring.application.instance-id}" // 현재 인스턴스의 group id
    )
    public void onNotificationCreated(NotificationCreatedEvent event) {
        NotificationPayload payload = event.notificationPayload();
        sseSender.sendToUser(payload.receiverId(),
                SseEmitter.event()
                        .id(payload.id().toString())
                        .name("notifications")
                        .data(payload));
    }

    // 비활성 대화에 DM이 도착하면 SSE "direct-messages" 이벤트로 전송 (알림 저장과 독립)
    @Async("dmEventExecutor")
    @EventListener
    public void onDirectMessageSse(DirectMessageSseEvent event) {
        DirectMessageResponse message = event.message();
        sseSender.sendToUser(message.receiver().id(),
                SseEmitter.event()
                        .id(message.id().toString())
                        .name("direct-messages")
                        .data(message));
    }
}
