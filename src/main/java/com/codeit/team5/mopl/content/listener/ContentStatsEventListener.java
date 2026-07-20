package com.codeit.team5.mopl.content.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.codeit.team5.mopl.content.repository.ContentStatsRepository;
import com.codeit.team5.mopl.watcher.event.WatcherJoinedEvent;
import com.codeit.team5.mopl.watcher.event.WatcherLeftEvent;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

@Component
@Transactional
@RequiredArgsConstructor
public class ContentStatsEventListener {

    private final ContentStatsRepository statsRepository;

    @EventListener
    public void handle(WatcherJoinedEvent event) {
        statsRepository.increaseWatcherCountById(event.contentId(), Instant.now());
    }

    @EventListener
    public void handle(WatcherLeftEvent event) {
        statsRepository.decreaseWatcherCountById(event.contentId(), Instant.now());
    }
}
