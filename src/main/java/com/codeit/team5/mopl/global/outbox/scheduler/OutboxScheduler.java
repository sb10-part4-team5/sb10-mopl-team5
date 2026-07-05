package com.codeit.team5.mopl.global.outbox.scheduler;

import java.time.Duration;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.codeit.team5.mopl.user.event.UserLockedEvent;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final IncompleteEventPublications incompleteEvents;
    private final CompletedEventPublications completedEvents;

    @Scheduled(fixedDelay = 60 * 1000)
    public void retryIncompleteEvents() {
        incompleteEvents.resubmitIncompletePublications(
                publication -> publication.getEvent().getClass().equals(UserLockedEvent.class));
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void cleanUpCompletedEvents() {
        completedEvents.deletePublicationsOlderThan(Duration.ofDays(7));
    }
}
