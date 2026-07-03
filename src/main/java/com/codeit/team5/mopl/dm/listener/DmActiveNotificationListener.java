package com.codeit.team5.mopl.dm.listener;

import com.codeit.team5.mopl.dm.event.DirectMessageBroadcastEvent;
import com.codeit.team5.mopl.dm.event.InactiveDirectMessageEvent;
import com.codeit.team5.mopl.dm.provider.ActiveConversationChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DmActiveNotificationListener {

    private final ActiveConversationChecker activeConversationChecker;
    private final ApplicationEventPublisher eventPublisher;

    // 비활성 게이트: 수신자가 대화방을 보고 있지 않으면 비활성 DM 이벤트를 발행한다.
    // 알림 저장 리스너와 SSE 전송 리스너가 이 이벤트를 각각 독립적으로 구독한다.
    @EventListener
    public void onDirectMessageBroadcast(DirectMessageBroadcastEvent event) {
        if (!activeConversationChecker.isViewing(event.message().conversationId(), event.receiverEmail())) {
            eventPublisher.publishEvent(new InactiveDirectMessageEvent(event.message()));
        }
    }
}
