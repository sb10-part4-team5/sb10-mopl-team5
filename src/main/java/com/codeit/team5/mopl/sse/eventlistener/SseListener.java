package com.codeit.team5.mopl.sse.eventlistener;

import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.dm.event.DirectMessageSseEvent;
import com.codeit.team5.mopl.global.infra.kafka.topic.KafkaTopics;
import com.codeit.team5.mopl.notification.dto.NotificationPayload;
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
    // Value는 JsonDeserializer를 사용해 JSON 데이터를 NotificationPayload 객체로 역직렬화한다.
    // 보안을 위해 com.codeit.qteam5.mopl.* 패키지의 클래스만 역직렬화를 허용한다.
    @KafkaListener(
            topics = KafkaTopics.NOTIFICATION_SSE,
            groupId = "sse-${spring.application.instance-id}", // 현재 인스턴스의 group id
            properties = {
                "value.deserializer=org.springframework.kafka.support.serializer.JsonDeserializer",
                "spring.json.trusted.packages=com.codeit.team5.mopl.*"
            }
    )
    public void onNotificationCreated(NotificationPayload payload) {
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
