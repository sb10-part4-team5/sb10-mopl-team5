package com.codeit.team5.mopl.notification.event;

import com.codeit.team5.mopl.notification.dto.NotificationResponse;

// 알림이 생성된 뒤 발행될 이벤트
// 추후 sse를 통한 알림 전송에 사용됨.
public record NotificationCreatedEvent(
    NotificationResponse notification
) {

}
