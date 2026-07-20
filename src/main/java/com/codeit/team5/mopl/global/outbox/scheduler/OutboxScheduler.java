package com.codeit.team5.mopl.global.outbox.scheduler;

import com.codeit.team5.mopl.global.outbox.event.RetryableOutboxEvent;
import com.codeit.team5.mopl.user.event.UserLockedEvent;
import com.codeit.team5.mopl.watcher.event.WatcherJoinedEvent;
import com.codeit.team5.mopl.watcher.event.WatcherLeftEvent;
import java.time.Duration;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
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
    @SchedulerLock(name = "outboxRetryIncompleteEvents", lockAtMostFor = "2m", lockAtLeastFor = "50s")
    public void retryIncompleteEvents() {
        incompleteEvents.resubmitIncompletePublications(
                publication -> publication.getEvent() instanceof RetryableOutboxEvent);
    }

    @Scheduled(cron = "0 0 1 * * *")
    @SchedulerLock(name = "outboxCleanupCompletedEvents", lockAtMostFor = "30m", lockAtLeastFor = "5m")
    public void cleanUpCompletedEvents() {
        completedEvents.deletePublicationsOlderThan(Duration.ofDays(7));
    }
}
