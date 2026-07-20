package com.codeit.team5.mopl.notification.event;

import com.codeit.team5.mopl.global.infra.kafka.topic.KafkaTopics;
import com.codeit.team5.mopl.notification.dto.NotificationPayload;
import org.springframework.modulith.events.Externalized;

// 알림이 생성된 뒤 발행될 이벤트
// @Externalized: Spring Modulith가 트랜잭션 커밋 후 Kafka topic으로 자동 발행한다.
// 모든 인스턴스가 동일 토픽을 consume하여 SSE 전달 → 스케일아웃 환경에서도 누락 없음.
@Externalized(KafkaTopics.NOTIFICATION_SSE)
public record NotificationCreatedEvent(
    NotificationPayload notificationPayload
) {

}
