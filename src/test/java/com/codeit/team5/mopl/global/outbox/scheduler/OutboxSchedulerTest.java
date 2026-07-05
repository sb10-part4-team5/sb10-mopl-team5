package com.codeit.team5.mopl.global.outbox.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.modulith.events.IncompleteEventPublications;

@ExtendWith(MockitoExtension.class)
class OutboxSchedulerTest {

    @Mock
    private IncompleteEventPublications incompleteEvents;

    @Mock
    private CompletedEventPublications completedEvents;

    @InjectMocks
    private OutboxScheduler scheduler;

    @Test
    @DisplayName("retryIncompleteEvents 호출 시 UserLockedEvent 타입의 미완료 이벤트가 재시도된다")
    void retryIncompleteEvents() {
        // when
        scheduler.retryIncompleteEvents();

        // then
        verify(incompleteEvents).resubmitIncompletePublications(any());
    }

    @Test
    @DisplayName("cleanUpCompletedEvents 호출 시 7일 이전의 완료된 이벤트가 삭제된다")
    void cleanUpCompletedEvents() {
        // when
        scheduler.cleanUpCompletedEvents();

        // then
        verify(completedEvents).deletePublicationsOlderThan(Duration.ofDays(7));
    }
}
