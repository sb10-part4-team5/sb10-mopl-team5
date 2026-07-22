package com.codeit.team5.mopl.dm.listener;

import com.codeit.team5.mopl.dm.constant.DmRedisConstants;
import com.codeit.team5.mopl.dm.event.DirectMessageBroadcastEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class DmBroadcastListener {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    // 메시지 저장 커밋 후에만 구독자에게 전송 (저장-전송 정합성 보장)
    @Async("dmEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDirectMessageBroadcast(DirectMessageBroadcastEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event.message());
            stringRedisTemplate.convertAndSend(DmRedisConstants.DM_BROADCAST_TOPIC, message);
        } catch (Exception e) {
            log.error("DM broadcast failed: conversationId={}", event.message().conversationId(), e);
        }
    }
}
