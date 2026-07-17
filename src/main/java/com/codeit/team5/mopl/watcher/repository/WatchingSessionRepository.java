package com.codeit.team5.mopl.watcher.repository;

import com.codeit.team5.mopl.global.infra.redis.repository.RedisRepository;
import com.codeit.team5.mopl.watcher.entity.WatchingSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Repository;

@Repository
public class WatchingSessionRepository extends RedisRepository<WatchingSession> {

    private static final String WATCHER_SESSION_KEY = "watcher:session:";
    private static final String CONTENT_WATCHERS_KEY = "content:%s:watchers";

    private final StringRedisTemplate stringRedisTemplate;

    public WatchingSessionRepository(RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper, StringRedisTemplate stringRedisTemplate) {
        super(connectionFactory, objectMapper, WatchingSession.class);
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 단건 및 페이징 키에 동시에 저장합니다.
     */
    public void save(WatchingSession session) {
        String singleKey = WATCHER_SESSION_KEY + session.watcherId();
        String sortedSetKey = String.format(CONTENT_WATCHERS_KEY, session.contentId());

        // 단건 저장 (Value 형태) - 12시간 TTL 부여
        super.set(singleKey, session, 12, TimeUnit.HOURS);

        // 페이징용 Sorted Set 저장 (Member: watcherId.toString(), Score: createdAt timestamp)
        Instant createdAt = session.createdAt();
        Double score = (createdAt != null) ? createdAt.toEpochMilli() : 0.0;

        stringRedisTemplate.opsForZSet().add(sortedSetKey, session.watcherId().toString(), score);
    }

    /**
     * 두 Key에서 모두 데이터를 삭제합니다.
     */
    public void deleteByContentIdAndWatcherId(UUID contentId, UUID watcherId) {
        String singleKey = WATCHER_SESSION_KEY + watcherId;
        String sortedSetKey = String.format(CONTENT_WATCHERS_KEY, contentId);

        super.delete(singleKey);

        stringRedisTemplate.opsForZSet().remove(sortedSetKey, watcherId.toString());
    }

    /**
     * 단건 Key를 통해 세션 조회합니다.
     */
    public Optional<WatchingSession> findByWatcherId(UUID watcherId) {
        String singleKey = WATCHER_SESSION_KEY + watcherId;
        return super.get(singleKey);
    }

    /**
     * 페이징용 Sorted Set에서 limit 개수만큼 WatchingSession 목록을 반환합니다. ZSet의 Score가 createdAt timestamp이므로
     * 단건 Key 조회 없이 바로 객체를 생성할 수 있습니다.
     */
    public List<WatchingSession> findWatchingSessionsByContentId(UUID contentId, int limit,
            Double maxScore) {
        String sortedSetKey = String.format(CONTENT_WATCHERS_KEY, contentId);

        Set<TypedTuple<String>> members = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(sortedSetKey, 0.0, maxScore, 0, limit);

        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }

        return members.stream()
                .map(tuple -> getWatchingSession(contentId, tuple))
                .toList();
    }

    /**
     * ZSet의 size를 반환합니다.
     */
    public Long countByContentId(UUID contentId) {
        String sortedSetKey = String.format(CONTENT_WATCHERS_KEY, contentId);
        return stringRedisTemplate.opsForZSet().zCard(sortedSetKey);
    }

    /**
     * 특정 watcherId가 해당 콘텐츠에 대해 세션이 존재하는지 확인합니다.
     */
    public boolean existsByContentIdAndWatcherId(UUID contentId, UUID watcherId) {
        String sortedSetKey = String.format(CONTENT_WATCHERS_KEY, contentId);
        return stringRedisTemplate.opsForZSet().score(sortedSetKey, watcherId.toString()) != null;
    }

    /**
     * 일정 시간이 지난 세션들을 ZSet에서 일괄 정리합니다.
     */
    public void cleanupOldSessions(long thresholdMillis) {
        Set<String> keys = stringRedisTemplate.keys(String.format(CONTENT_WATCHERS_KEY, "*"));
        keys.forEach(key -> stringRedisTemplate.opsForZSet()
                .removeRangeByScore(key, 0, thresholdMillis));
    }

    /**
     * 특정 콘텐츠의 모든 세션을 제거합니다.
     */
    public void deleteAllByContentId(UUID contentId) {
        String sortedSetKey = String.format(CONTENT_WATCHERS_KEY, contentId);
        Set<String> members = stringRedisTemplate.opsForZSet().range(sortedSetKey, 0, -1);
        if (members == null || members.isEmpty()) {
            stringRedisTemplate.delete(sortedSetKey);
        }
        List<String> singleKeys = members.stream()
                .map(watcherId -> WATCHER_SESSION_KEY + watcherId)
                .toList();
        stringRedisTemplate.delete(singleKeys);
    }

    private WatchingSession getWatchingSession(UUID contentId,
            TypedTuple<String> tuple) {
        return new WatchingSession(
                UUID.fromString(tuple.getValue()),
                contentId,
                Instant.ofEpochMilli(tuple.getScore().longValue()));
    }
}
