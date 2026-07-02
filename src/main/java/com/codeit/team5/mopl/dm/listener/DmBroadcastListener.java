package com.codeit.team5.mopl.dm.listener;

import com.codeit.team5.mopl.dm.event.DirectMessageBroadcastEvent;
import com.codeit.team5.mopl.dm.provider.DirectMessageBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class DmBroadcastListener {

    private final DirectMessageBroadcaster directMessageBroadcaster;

    // 메시지 저장 커밋 후에만 구독자에게 전송 (저장-전송 정합성 보장)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDirectMessageBroadcast(DirectMessageBroadcastEvent event) {
        try {
            directMessageBroadcaster.broadcast(event.message());
        } catch (Exception e) {
            log.error("DM broadcast failed: conversationId={}", event.message().conversationId(), e);
        }
    }
}
