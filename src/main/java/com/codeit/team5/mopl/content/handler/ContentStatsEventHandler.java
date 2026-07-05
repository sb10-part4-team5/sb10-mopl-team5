package com.codeit.team5.mopl.content.handler;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.codeit.team5.mopl.content.repository.ContentStatsRepository;
import com.codeit.team5.mopl.watcher.event.WatcherJoinedEvent;
import com.codeit.team5.mopl.watcher.event.WatcherLeftEvent;
import lombok.RequiredArgsConstructor;

@Component
@Transactional
@RequiredArgsConstructor
public class ContentStatsEventHandler {

    private final ContentStatsRepository statsRepository;

    @EventListener
    public void handle(WatcherJoinedEvent event) {
        statsRepository.increaseWatcherCountById(event.contentId());
    }

    @EventListener
    public void handle(WatcherLeftEvent event) {
        statsRepository.decreaseWatcherCountById(event.contentId());
    }
}
