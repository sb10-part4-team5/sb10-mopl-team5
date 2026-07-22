package com.codeit.team5.mopl.dm.listener;

import com.codeit.team5.mopl.dm.event.DirectMessageBroadcastEvent;
import com.codeit.team5.mopl.dm.event.DirectMessageNotificationEvent;
import com.codeit.team5.mopl.dm.event.DirectMessageSseEvent;
import com.codeit.team5.mopl.dm.provider.ActiveConversationChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DmActiveNotificationListener {

    private final ActiveConversationChecker activeConversationChecker;
    private final ApplicationEventPublisher eventPublisher;

    // 비활성 게이트: 커밋 후 수신자가 대화방을 보고 있지 않으면 알림·SSE 이벤트를 각각 발행한다.
    // AFTER_COMMIT 리스너에서 호출되므로, DirectMessageSseEvent의 Kafka 외부화(커밋 훅 기반)가
    // 실제로 트리거되려면 활성 트랜잭션이 필요해 REQUIRES_NEW로 새 물리 트랜잭션을 보장한다.
    @Async("dmEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onDirectMessageBroadcast(DirectMessageBroadcastEvent event) {
        if (!activeConversationChecker.isViewing(event.message().conversationId(), event.receiverId())) {
            eventPublisher.publishEvent(new DirectMessageNotificationEvent(event.message()));
            eventPublisher.publishEvent(new DirectMessageSseEvent(event.message()));
        }
    }
}
