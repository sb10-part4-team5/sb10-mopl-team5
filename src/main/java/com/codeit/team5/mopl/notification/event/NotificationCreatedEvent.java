package com.codeit.team5.mopl.notification.event;

import com.codeit.team5.mopl.global.infra.kafka.topic.KafkaTopics;
import com.codeit.team5.mopl.notification.dto.NotificationPayload;
import org.springframework.modulith.events.Externalized;

// 알림이 생성된 뒤 발행될 이벤트
// @Externalized: Spring Modulith가 트랜잭션 커밋 후 Kafka topic으로 자동 발행한다.
// 모든 인스턴스가 동일 토픽을 consume하여 SSE 전달한다.
// SSE는 실시간 best-effort 전달이므로 재시작 구간의 짧은 누락은 허용 범위이다.
// 알림 영속성은 DB에 보장되며, 누락된 알림은 클라이언트가 목록 조회로 보완한다.
@Externalized(KafkaTopics.NOTIFICATION_CREATED_SSE
    + "::#{#this.notificationPayload().receiverId()}"
)
public record NotificationCreatedEvent(
    NotificationPayload notificationPayload
) {

}
