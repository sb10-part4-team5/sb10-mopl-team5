package com.codeit.team5.mopl.auth.jwt.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.global.outbox.event.RetryableOutboxEvent;
import com.codeit.team5.mopl.global.outbox.scheduler.OutboxScheduler;
import com.codeit.team5.mopl.user.event.RoleChangedEvent;
import com.codeit.team5.mopl.user.event.UserLockedEvent;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class AuthUserCacheOutboxIntegrationTest {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OutboxScheduler outboxScheduler;

    @Autowired
    @Qualifier("outboxEventWorker")
    private Executor outboxEventWorker;

    @MockitoBean
    private AuthUserCacheStore authUserCacheStore;

    @Test
    void successfulAsyncListener_marksPublicationCompleted() throws InterruptedException {
        UserLockedEvent event = new UserLockedEvent(UUID.randomUUID(), false);
        CountDownLatch invocation = new CountDownLatch(1);
        doAnswer(call -> {
            invocation.countDown();
            return null;
        }).when(authUserCacheStore).evict(event.id());

        publishInTransaction(event);

        assertThat(invocation.await(10, TimeUnit.SECONDS)).isTrue();
        awaitCondition(() -> publicationCount(event.id(), true) == 1);
        assertThat(publicationCount(event.id(), false)).isZero();
    }

    @Test
    void failedAsyncListener_remainsIncompleteAndCompletesAfterSchedulerRetry()
            throws InterruptedException {
        UserLockedEvent event = new UserLockedEvent(UUID.randomUUID(), false);
        RuntimeException failure = new RuntimeException("redis unavailable");
        CountDownLatch firstInvocation = new CountDownLatch(1);
        doAnswer(call -> {
            firstInvocation.countDown();
            throw failure;
        }).when(authUserCacheStore).evict(event.id());

        publishInTransaction(event);

        assertThat(firstInvocation.await(10, TimeUnit.SECONDS)).isTrue();
        awaitCondition(() -> publicationCount(event.id(), false)
                + publicationCount(event.id(), true) == 1);
        assertThat(publicationCount(event.id(), false)).isEqualTo(1);
        assertThat(new RoleChangedEvent(UUID.randomUUID(), "USER", "ADMIN"))
                .isInstanceOf(RetryableOutboxEvent.class);
        assertThat(event)
                .isInstanceOf(RetryableOutboxEvent.class);
        awaitCondition(this::outboxWorkerIsIdle);

        CountDownLatch retryInvocation = new CountDownLatch(1);
        reset(authUserCacheStore);
        doAnswer(call -> {
            retryInvocation.countDown();
            return null;
        }).when(authUserCacheStore).evict(event.id());

        OutboxScheduler schedulerTarget = AopTestUtils.getTargetObject(outboxScheduler);
        schedulerTarget.retryIncompleteEvents();

        assertThat(retryInvocation.await(10, TimeUnit.SECONDS)).isTrue();
        awaitCondition(() -> publicationCount(event.id(), true) == 1);
        assertThat(publicationCount(event.id(), false)).isZero();
    }

    private void publishInTransaction(Object event) {
        transactionTemplate.executeWithoutResult(status -> publisher.publishEvent(event));
    }

    private int publicationCount(UUID eventId, boolean completed) {
        String completionCondition = completed
                ? "completion_date is not null"
                : "completion_date is null";
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from event_publication "
                        + "where serialized_event like ? "
                        + "and listener_id like ? and " + completionCondition,
                Integer.class,
                "%" + eventId + "%",
                "%AuthUserCacheEvictionListener%"
        );
        return count == null ? 0 : count;
    }

    private boolean outboxWorkerIsIdle() {
        if (!(outboxEventWorker instanceof ThreadPoolTaskExecutor executor)) {
            return true;
        }
        return executor.getActiveCount() == 0 && executor.getQueueSize() == 0;
    }

    private void awaitCondition(BooleanSupplier condition) {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            while (!condition.getAsBoolean()) {
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
            }
        });
    }
}
