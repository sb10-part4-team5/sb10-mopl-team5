package com.codeit.team5.mopl.sse.eventlistener;

import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.dm.event.DirectMessageSseEvent;
import com.codeit.team5.mopl.global.infra.kafka.topic.KafkaTopics;
import com.codeit.team5.mopl.notification.dto.NotificationPayload;
import com.codeit.team5.mopl.notification.event.NotificationCreatedEvent;
import com.codeit.team5.mopl.notification.event.NotificationsBatchCreatedEvent;
import com.codeit.team5.mopl.sse.sender.SseSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@RequiredArgsConstructor
@Slf4j
public class SseListener {

    private final SseSender sseSender;
    private final ThreadPoolTaskExecutor notificationBatchSseExecutor;

    // Kafka의 NOTIFICATION_SSE 토픽을 구독하는 리스너
    // 각 인스턴스가 고유한 groupId를 사용하므로 모든 인스턴스가 동일한 이벤트를 consume한다.
    // Spring Modulith @Externalized는 NotificationCreatedEvent 전체를 JSON 바이트로 직렬화한다.
    // ByteArrayDeserializer(yml 기본값)로 byte[]를 받고, JsonMessageConverter가 메서드
    // 파라미터 타입(NotificationCreatedEvent)을 추론해 역직렬화한다.
    // JsonDeserializer를 properties로 명시하면 JsonMessageConverter와 이중 변환 충돌 발생.
    // SSE는 실시간 전달만 필요하므로 재시작 시 과거 이력을 재처리하지 않도록 latest 고정.
    // (earliest이면 재시작마다 새 UUID 그룹이 offset 0부터 읽어 신규 메시지 처리가 지연됨)
    @KafkaListener(
            topics = KafkaTopics.NOTIFICATION_CREATED_SSE,
            groupId = "sse-${spring.application.instance-id}", // 현재 인스턴스의 group id
            properties = {"auto.offset.reset=latest"}
    )
    public void onNotificationCreated(NotificationCreatedEvent event) {
        NotificationPayload payload = event.notificationPayload();
        //log.debug("[SSE] Kafka 메시지 수신: receiverId={}, type={}", payload.receiverId(), payload.type());
        sseSender.sendToUser(payload.receiverId(),
                SseEmitter.event()
                        .id(payload.id().toString())
                        .name("notifications")
                        .data(payload));
    }

    @KafkaListener(
        topics = KafkaTopics.NOTIFICATION_BATCH_CREATED_SSE,
        groupId = "sse-${spring.application.instance-id}", // 현재 인스턴스의 group id
        properties = {"auto.offset.reset=latest"}
    )
    public void onNotificationBatchCreated(NotificationsBatchCreatedEvent event) {
        // @Async는 KafkaListener에 적용되지 않으므로 executor에 직접 제출하여 consumer 스레드를 즉시 해방한다.
        for (NotificationPayload payload : event.payloads()) {
            notificationBatchSseExecutor.execute(() ->
                sseSender.sendToUser(payload.receiverId(),
                    SseEmitter.event()
                        .id(payload.id().toString())
                        .name("notifications")
                        .data(payload)));
        }
    }

    // 비활성 대화에 DM이 도착하면 SSE "direct-messages" 이벤트로 전송 (알림 저장과 독립)
    @KafkaListener(
            topics = KafkaTopics.DIRECT_MESSAGE_SSE,
            groupId = "sse-${spring.application.instance-id}",
            properties = {"auto.offset.reset=latest"}
    )
    public void onDirectMessageSse(DirectMessageSseEvent event) {
        DirectMessageResponse message = event.message();
        sseSender.sendToUser(message.receiver().id(),
                SseEmitter.event()
                        .id(message.id().toString())
                        .name("direct-messages")
                        .data(message));
    }
}
