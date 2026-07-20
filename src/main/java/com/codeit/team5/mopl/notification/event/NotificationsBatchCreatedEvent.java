package com.codeit.team5.mopl.notification.event;

import com.codeit.team5.mopl.global.infra.kafka.topic.KafkaTopics;
import com.codeit.team5.mopl.notification.dto.NotificationPayload;
import java.util.List;
import org.springframework.modulith.events.Externalized;

// 여러 알림이 한 번에 생성될 때 발행되는 배치 이벤트
@Externalized(KafkaTopics.NOTIFICATION_SSE)
public record NotificationsBatchCreatedEvent(
    List<NotificationPayload> payloads
) {

}
