package com.codeit.team5.mopl.watcher.repository;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.watcher.entity.WatchingSession;

@DataRedisTest
@Import({TestcontainersConfiguration.class, WatchingSessionRepository.class, JacksonAutoConfiguration.class})
class WatchingSessionRepositoryTest {

    @Autowired
    private WatchingSessionRepository repository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        stringRedisTemplate.execute((RedisConnection connection) -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }

    @Test
    @DisplayName("save - session is saved to both single key and sorted set")
    void save_success() {
        UUID watcherId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        Instant now = Instant.now();
        WatchingSession session = new WatchingSession(watcherId, contentId, now);

        repository.save(session);

        Optional<WatchingSession> saved = repository.findByWatcherId(watcherId);
        assertThat(saved).isPresent();
        assertThat(saved.get().contentId()).isEqualTo(contentId);

        Long count = repository.countByContentId(contentId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("findByWatcherId - returns session if exists")
    void findByWatcherId_success() {
        UUID watcherId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        Instant now = Instant.now();
        WatchingSession session = new WatchingSession(watcherId, contentId, now);
        repository.save(session);

        Optional<WatchingSession> result = repository.findByWatcherId(watcherId);

        assertThat(result).isPresent();
        assertThat(result.get().contentId()).isEqualTo(contentId);
    }

    @Test
    @DisplayName("findByWatcherId - returns empty if not exists")
    void findByWatcherId_NotFound() {
        Optional<WatchingSession> result = repository.findByWatcherId(UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findWatchingSessionsByContentId - 커서를 이용해 이전 시점의 세션들을 조회한다")
    void findWatchingSessionsByContentId_cursor_success() {
        UUID contentId = UUID.randomUUID();
        Instant now = Instant.now();

        // 3개의 세션을 명시적인 Timestamp로 저장 (score가 다르게 저장됨)
        repository.save(new WatchingSession(UUID.randomUUID(), contentId, now.minusSeconds(30)));
        repository.save(new WatchingSession(UUID.randomUUID(), contentId, now.minusSeconds(20)));

        Instant lastTime = now.minusSeconds(10);
        WatchingSession lastSession = new WatchingSession(UUID.randomUUID(), contentId, lastTime);
        repository.save(lastSession);

        // 첫 페이지 조회 (최신순 2개)
        List<WatchingSession> firstPage = repository.findWatchingSessionsByContentId(contentId, 2, Range.closed(0.0, Double.MAX_VALUE));
        assertThat(firstPage).hasSize(2);

        // 마지막 항목의 createdAt 값을 cursor로 사용 (마지막 항목도 포함해서 조회하므로 maxScore는 inclusive)
        // 실제 서비스에서는 limit + 1을 조회한 후, (마지막 요소의 timestamp - 1) 또는 동일한 score를 사용해 처리함
        Double maxScore = (double) firstPage.get(1).createdAt().toEpochMilli();

        // 다음 페이지 조회
        List<WatchingSession> nextPage = repository.findWatchingSessionsByContentId(contentId, 2, Range.closed(0.0, maxScore));

        // inclusive로 인해 겹치는 항목이 포함되어 2개가 조회됨 (총 3개 중 2번째, 1번째)
        assertThat(nextPage).hasSize(2);
        assertThat(nextPage.get(0).createdAt()).isEqualTo(firstPage.get(1).createdAt());
    }

    @Test
    @DisplayName("deleteByContentIdAndWatcherId - deletes session")
    void deleteByContentIdAndWatcherId_success() {
        UUID watcherId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        repository.save(new WatchingSession(watcherId, contentId, Instant.now()));

        repository.deleteByContentIdAndWatcherId(contentId, watcherId);

        assertThat(repository.findByWatcherId(watcherId)).isEmpty();
        assertThat(repository.countByContentId(contentId)).isEqualTo(0);
    }

    @Test
    @DisplayName("existsByContentIdAndWatcherId - success")
    void existsByContentIdAndWatcherId_success() {
        UUID watcherId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        repository.save(new WatchingSession(watcherId, contentId, Instant.now()));

        boolean exists = repository.existsByContentIdAndWatcherId(contentId, watcherId);
        assertThat(exists).isTrue();
    }
    @Test
    @DisplayName("cleanupOldSessions - deletes sessions older than threshold from ZSet")
    void cleanupOldSessions_success() {
        UUID contentId = UUID.randomUUID();
        UUID oldWatcherId = UUID.randomUUID();
        UUID recentWatcherId = UUID.randomUUID();

        // 오래된 세션 저장 (score가 임계값보다 작게 저장되도록 설정)
        repository.save(new WatchingSession(oldWatcherId, contentId, Instant.now().minusSeconds(100)));
        
        long thresholdMillis = Instant.now().minusSeconds(50).toEpochMilli();
        
        // 최근 세션 저장
        repository.save(new WatchingSession(recentWatcherId, contentId, Instant.now()));

        // threshold 이하의 세션들을 ZSet에서 제거
        repository.cleanupOldSessions(thresholdMillis);

        // ZSet에서 오래된 세션은 삭제되었으므로 전체 개수는 1개여야 함
        assertThat(repository.countByContentId(contentId)).isEqualTo(1);
        // 남아있는 세션은 최근 세션인지 확인
        assertThat(repository.existsByContentIdAndWatcherId(contentId, recentWatcherId)).isTrue();
        assertThat(repository.existsByContentIdAndWatcherId(contentId, oldWatcherId)).isFalse();
    }

    @Test
    @DisplayName("deleteAllByContentId - deletes all sessions for specific content")
    void deleteAllByContentId_success() {
        UUID contentId1 = UUID.randomUUID();
        UUID contentId2 = UUID.randomUUID();
        UUID watcherId1 = UUID.randomUUID();
        UUID watcherId2 = UUID.randomUUID();
        UUID watcherId3 = UUID.randomUUID();

        repository.save(new WatchingSession(watcherId1, contentId1, Instant.now()));
        repository.save(new WatchingSession(watcherId2, contentId1, Instant.now()));
        repository.save(new WatchingSession(watcherId3, contentId2, Instant.now()));

        repository.deleteAllByContentId(contentId1);

        // contentId1 에 대한 단일 세션 키는 모두 삭제되어야 함
        assertThat(repository.findByWatcherId(watcherId1)).isEmpty();
        assertThat(repository.findByWatcherId(watcherId2)).isEmpty();
        
        // ZSet 도 비워져야 하므로 카운트는 0이어야 함
        assertThat(repository.countByContentId(contentId1)).isEqualTo(0);

        // contentId2 에 대한 세션은 남아있어야 함
        assertThat(repository.findByWatcherId(watcherId3)).isPresent();
        assertThat(repository.countByContentId(contentId2)).isEqualTo(1);
    }

    @Test
    @DisplayName("findWatchingSessionsByContentId - 세션이 없을 경우 빈 리스트 반환")
    void findWatchingSessionsByContentId_Empty() {
        List<WatchingSession> result = repository.findWatchingSessionsByContentId(UUID.randomUUID(), 10, Range.closed(0.0, 100.0));
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsByContentIdAndWatcherId - 존재하지 않을 경우 false 반환")
    void existsByContentIdAndWatcherId_False() {
        boolean exists = repository.existsByContentIdAndWatcherId(UUID.randomUUID(), UUID.randomUUID());
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("deleteAllByContentId - ZSet이 비어있을 경우 정상 처리")
    void deleteAllByContentId_Empty() {
        UUID contentId = UUID.randomUUID();
        repository.deleteAllByContentId(contentId);
        assertThat(repository.countByContentId(contentId)).isEqualTo(0);
    }
}
