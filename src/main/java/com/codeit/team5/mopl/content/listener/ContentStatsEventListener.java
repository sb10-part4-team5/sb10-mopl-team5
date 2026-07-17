package com.codeit.team5.mopl.content.listener;

import com.codeit.team5.mopl.content.repository.ContentStatsRepository;
import com.codeit.team5.mopl.watcher.event.WatcherJoinedEvent;
import com.codeit.team5.mopl.watcher.event.WatcherLeftEvent;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Transactional
@RequiredArgsConstructor
public class ContentStatsEventListener {

    private final ContentStatsRepository statsRepository;

    @Async("outboxEventWorker")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener
    public void handle(WatcherJoinedEvent event) {
        statsRepository.increaseWatcherCountById(event.contentId(), Instant.now());
    }

    @Async("outboxEventWorker")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener
    public void handle(WatcherLeftEvent event) {
        statsRepository.decreaseWatcherCountById(event.contentId(), Instant.now());
    }
}
