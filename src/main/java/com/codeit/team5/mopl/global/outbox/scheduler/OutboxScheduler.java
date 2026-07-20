package com.codeit.team5.mopl.global.outbox.scheduler;

import com.codeit.team5.mopl.global.outbox.event.RetryableOutboxEvent;
import com.codeit.team5.mopl.user.event.UserLockedEvent;
import com.codeit.team5.mopl.watcher.event.WatcherJoinedEvent;
import com.codeit.team5.mopl.watcher.event.WatcherLeftEvent;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final IncompleteEventPublications incompleteEvents;
    private final CompletedEventPublications completedEvents;

    @Scheduled(fixedDelay = 60 * 1000)
    public void retryIncompleteEvents() {
        incompleteEvents.resubmitIncompletePublications(
                publication -> publication.getEvent() instanceof RetryableOutboxEvent);
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void cleanUpCompletedEvents() {
        completedEvents.deletePublicationsOlderThan(Duration.ofDays(7));
    }
}
