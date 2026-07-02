package com.codeit.team5.mopl.dm.listener;

import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.dm.event.DirectMessageBroadcastEvent;
import com.codeit.team5.mopl.dm.provider.ActiveConversationChecker;
import com.codeit.team5.mopl.notification.event.DirectMessageSentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DmActiveNotificationListener {

    private final ActiveConversationChecker activeConversationChecker;
    private final ApplicationEventPublisher eventPublisher;

    // 수신자가 대화방을 보고 있지 않을 때(비활성)만 알림 발행
    @EventListener
    public void onDirectMessageBroadcast(DirectMessageBroadcastEvent event) {
        DirectMessageResponse message = event.message();
        if (!activeConversationChecker.isViewing(message.conversationId(), event.receiverEmail())) {
            eventPublisher.publishEvent(new DirectMessageSentEvent(
                    message.receiver().userId(),
                    message.sender().name(),
                    message.content()
            ));
        }
    }
}
