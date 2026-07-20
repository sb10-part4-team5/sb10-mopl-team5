package com.codeit.team5.mopl.watcher.scheduler;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.watcher.repository.WatchingSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WatchingSessionCleanupSchedulerTest {

    @Mock
    private WatchingSessionRepository redisRepository;

    @InjectMocks
    private WatchingSessionCleanupScheduler scheduler;

    @Test
    @DisplayName("cleanupOldSessions - 스케줄러 실행 시 리포지토리 호출")
    void cleanupOldSessions_Success() {
        // when
        scheduler.cleanupOldSessions();

        // then
        verify(redisRepository).cleanupOldSessions(anyLong());
    }

    @Test
    @DisplayName("cleanupOldSessions - 리포지토리 예외 발생 시 에러 로깅 처리")
    void cleanupOldSessions_Exception() {
        // given
        doThrow(new RuntimeException("DB Error")).when(redisRepository).cleanupOldSessions(anyLong());

        // when
        scheduler.cleanupOldSessions();

        // then
        verify(redisRepository).cleanupOldSessions(anyLong());
    }
}
