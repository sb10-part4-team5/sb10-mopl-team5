package com.codeit.team5.mopl.dm.listener;

import com.codeit.team5.mopl.dm.event.DirectMessageBroadcastEvent;
import com.codeit.team5.mopl.dm.event.DirectMessageNotificationEvent;
import com.codeit.team5.mopl.dm.event.DirectMessageSseEvent;
import com.codeit.team5.mopl.dm.provider.ActiveConversationChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DmActiveNotificationListener {

    private final ActiveConversationChecker activeConversationChecker;
    private final ApplicationEventPublisher eventPublisher;

    // 비활성 게이트: 커밋 후 수신자가 대화방을 보고 있지 않으면 알림·SSE 이벤트를 각각 발행한다.
    @Async("dmEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDirectMessageBroadcast(DirectMessageBroadcastEvent event) {
        if (!activeConversationChecker.isViewing(event.message().conversationId(), event.receiverId())) {
            eventPublisher.publishEvent(new DirectMessageNotificationEvent(event.message()));
            eventPublisher.publishEvent(new DirectMessageSseEvent(event.message()));
        }
    }
}
