package com.codeit.team5.mopl.watcher.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.scheduling.annotation.Async;
import com.codeit.team5.mopl.content.event.ContentDeletedEvent;
import com.codeit.team5.mopl.user.event.UserLockedEvent;
import com.codeit.team5.mopl.watcher.entity.WatchingSession;
import com.codeit.team5.mopl.watcher.repository.WatchingSessionRepository;
import com.codeit.team5.mopl.watcher.service.WatchingSessionCommandService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WatchingSessionEventListener {

    private final WatchingSessionCommandService commandService;
    private final WatchingSessionRepository redisRepository;

    @Async("outboxEventWorker")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onContentDeleted(ContentDeletedEvent event) {
        commandService.clearContentSessions(event.contentId());
    }

    @Async("outboxEventWorker")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUserLocked(UserLockedEvent event) {
        if (event.locked()) {
            redisRepository.findByWatcherId(event.id())
                    .ifPresent(session -> commandService.left(session.contentId(), event.id()));
        }
    }
}
