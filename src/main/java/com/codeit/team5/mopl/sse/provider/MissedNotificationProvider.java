package com.codeit.team5.mopl.sse.provider;

import com.codeit.team5.mopl.notification.dto.NotificationPayload;
import java.util.List;
import java.util.UUID;

// SseService가 NotificationService 직접 참조를 피하기 위한 Provider
// port & adapt 패턴 적용
public interface MissedNotificationProvider {

    // 미수신된 알림 Payload들을 Payload로 뽑아옴
    List<NotificationPayload> findMissedNotifications(UUID userId, UUID lastEventId);

}
