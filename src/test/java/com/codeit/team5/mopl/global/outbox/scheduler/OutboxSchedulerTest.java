package com.codeit.team5.mopl.global.outbox.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Predicate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.modulith.events.EventPublication;
import org.springframework.modulith.events.IncompleteEventPublications;
import com.codeit.team5.mopl.user.event.UserLockedEvent;

@ExtendWith(MockitoExtension.class)
class OutboxSchedulerTest {

    @Mock
    private IncompleteEventPublications incompleteEvents;

    @Mock
    private CompletedEventPublications completedEvents;

    @InjectMocks
    private OutboxScheduler scheduler;

    @Captor
    private ArgumentCaptor<Predicate<EventPublication>> predicateCaptor;

    @Test
    @DisplayName("retryIncompleteEvents 호출 시 RetryableOutboxEvent 타입의 미완료 이벤트만 필터링하여 재시도한다")
    void retryIncompleteEvents() {
        // when
        scheduler.retryIncompleteEvents();

        // then
        verify(incompleteEvents).resubmitIncompletePublications(predicateCaptor.capture());
        Predicate<EventPublication> predicate = predicateCaptor.getValue();

        EventPublication lockedPublication = mock(EventPublication.class);
        given(lockedPublication.getEvent())
                .willReturn(new UserLockedEvent(UUID.randomUUID(), true));

        EventPublication otherPublication = mock(EventPublication.class);
        given(otherPublication.getEvent()).willReturn(new Object());

        assertThat(predicate.test(lockedPublication)).isTrue();
        assertThat(predicate.test(otherPublication)).isFalse();
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
