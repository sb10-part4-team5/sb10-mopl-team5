package com.codeit.team5.mopl.dm.listener;

import com.codeit.team5.mopl.dm.constant.DmRedisConstants;
import com.codeit.team5.mopl.dm.event.DirectMessageBroadcastEvent;
import com.codeit.team5.mopl.dm.exception.DmBroadcastException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class DmBroadcastListener {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    // 메시지 저장 커밋 후에만 구독자에게 전송 (저장-전송 정합성 보장)
    // REQUIRES_NEW: 재발행 경로(OutboxScheduler)에서도 AFTER_COMMIT 리스너가 호출되도록 보장
    @Async("dmEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onDirectMessageBroadcast(DirectMessageBroadcastEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event.message());
            stringRedisTemplate.convertAndSend(DmRedisConstants.DM_BROADCAST_TOPIC, message);
        } catch (JsonProcessingException e) {
            log.error("DM broadcast failed: conversationId={}", event.message().conversationId(), e);
            throw new DmBroadcastException("Failed to serialize DM broadcast message", e);
        }
    }
}
