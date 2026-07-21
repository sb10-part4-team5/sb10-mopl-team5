package com.codeit.team5.mopl.auth.jwt.cache;

import com.codeit.team5.mopl.global.exception.BusinessException;
import com.codeit.team5.mopl.user.event.RoleChangedEvent;
import com.codeit.team5.mopl.user.event.UserLockedEvent;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthUserCacheEvictionListener {

    private final AuthUserCacheStore authUserCacheStore;

    @Async("outboxEventWorker")
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void handle(RoleChangedEvent event) {
        evict(event.receiverId());
    }

    @Async("outboxEventWorker")
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void handle(UserLockedEvent event) {
        evict(event.id());
    }

    private void evict(UUID userId) {
        try {
            authUserCacheStore.evict(userId);
        } catch (BusinessException e) {
            log.warn("Auth user cache eviction failed: userId={}", userId, e);
            throw e;
        }
    }
}
