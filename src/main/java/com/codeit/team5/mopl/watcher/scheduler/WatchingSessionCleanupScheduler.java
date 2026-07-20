package com.codeit.team5.mopl.watcher.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import com.codeit.team5.mopl.watcher.repository.WatchingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class WatchingSessionCleanupScheduler {

    private final WatchingSessionRepository redisRepository;

    /**
     * 매 정각마다 12시간 이상 지난 ZSet의 유령 세션을 정리합니다.
     */
    @Scheduled(cron = "0 0 * * * *")
    @SchedulerLock(name = "WatchingSessionCleanupScheduler_cleanupOldSessions", lockAtLeastFor = "5m", lockAtMostFor = "15m")
    public void cleanupOldSessions() {
        long thresholdMillis = Instant.now().minus(12, ChronoUnit.HOURS).toEpochMilli();
        log.info("Starting ZSet cleanup for WatchingSessions older than {}", thresholdMillis);
        try {
            redisRepository.cleanupOldSessions(thresholdMillis);
            log.info("Finished ZSet cleanup for WatchingSessions");
        } catch (Exception e) {
            log.error("Failed to cleanup old WatchingSessions", e);
        }
    }
}
