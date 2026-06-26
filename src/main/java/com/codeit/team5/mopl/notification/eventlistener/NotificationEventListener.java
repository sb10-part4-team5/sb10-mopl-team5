package com.codeit.team5.mopl.notification.eventlistener;

import com.codeit.team5.mopl.notification.entity.NotificationLevel;
import com.codeit.team5.mopl.notification.entity.NotificationType;
import com.codeit.team5.mopl.notification.event.DirectMessageSentEvent;
import com.codeit.team5.mopl.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

// 여러 도메인에서 알림 생성이 트리거 될 때 작업을 수행하는 이벤트 리스너입니다.
@Component
@RequiredArgsConstructor
public class NotificationEventListener {
    private final NotificationService notificationService;

    public void onDirectMessageSent(DirectMessageSentEvent event){
        // 알림의 제목
        String title = "[DM] " + event.senderNickname();
        // DM 내용이 길면 50자로 줄여서 보여줌
        String content = StringUtils.truncate(event.content(), 50);
        notificationService.create(event.receiverId, NotificationType.DIRECT_MESSAGE,
            title, content, NotificationLevel.INFO);
    }


}
