package com.codeit.team5.mopl.dm.listener;

import com.codeit.team5.mopl.dm.event.DirectMessageBroadcastEvent;
import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DmBroadcastListener {

    private final SimpMessagingTemplate messagingTemplate;

    // 메시지 저장 커밋 후에만 구독자에게 전송 (저장-전송 정합성 보장)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDirectMessageBroadcast(DirectMessageBroadcastEvent event) {
        String destination = StompConstants.SUB_CONVERSATION_DM
                .replace("{id}", event.conversationId().toString());
        messagingTemplate.convertAndSend(destination, event.message());
    }
}
