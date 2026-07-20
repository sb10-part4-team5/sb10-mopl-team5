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
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.watcher.entity.WatchingSession;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;

@DataRedisTest
@Import({TestcontainersConfiguration.class, WatchingSessionRepository.class, JacksonAutoConfiguration.class})
class WatchingSessionRepositoryTest {

    @Autowired
    private WatchingSessionRepository repository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        stringRedisTemplate.getConnectionFactory().getConnection().flushAll();
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
    void findWatchingSessionsByContentId_cursor_success() throws InterruptedException {
        UUID contentId = UUID.randomUUID();
        
        // 3개의 세션을 시간차를 두고 저장 (score가 다르게 저장됨)
        repository.save(new WatchingSession(UUID.randomUUID(), contentId, Instant.now().minusSeconds(30)));
        Thread.sleep(5);
        repository.save(new WatchingSession(UUID.randomUUID(), contentId, Instant.now().minusSeconds(20)));
        Thread.sleep(5);
        
        Instant lastTime = Instant.now().minusSeconds(10);
        WatchingSession lastSession = new WatchingSession(UUID.randomUUID(), contentId, lastTime);
        repository.save(lastSession);

        // 첫 페이지 조회 (최신순 2개)
        List<WatchingSession> firstPage = repository.findWatchingSessionsByContentId(contentId, 2, Double.MAX_VALUE);
        assertThat(firstPage).hasSize(2);
        
        // 마지막 항목의 createdAt 값을 cursor로 사용 (마지막 항목도 포함해서 조회하므로 maxScore는 inclusive)
        // 실제 서비스에서는 limit + 1을 조회한 후, (마지막 요소의 timestamp - 1) 또는 동일한 score를 사용해 처리함
        Double maxScore = (double) firstPage.get(1).createdAt().toEpochMilli();
        
        // 다음 페이지 조회
        List<WatchingSession> nextPage = repository.findWatchingSessionsByContentId(contentId, 2, maxScore);
        
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
}
