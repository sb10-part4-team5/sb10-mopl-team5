package com.codeit.team5.mopl.watcher.repository;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import com.codeit.team5.mopl.global.infra.redis.repository.RedisRepository;
import com.codeit.team5.mopl.watcher.entity.WatchingSession;

@Repository
public class WatchingSessionRedisRepository extends RedisRepository<WatchingSession> {

    private static final String WATCHER_SESSION_KEY = "watcher:session:";
    private static final String CONTENT_WATCHERS_KEY = "content:%s:watchers";

    private final StringRedisTemplate stringRedisTemplate;

    public WatchingSessionRedisRepository(RedisConnectionFactory connectionFactory, StringRedisTemplate stringRedisTemplate) {
        super(connectionFactory, WatchingSession.class);
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 단건 및 페이징 키에 동시에 저장합니다.
     */
    public void save(WatchingSession session) {
        String singleKey = WATCHER_SESSION_KEY + session.watcherId();
        String sortedSetKey = String.format(CONTENT_WATCHERS_KEY, session.contentId());

        // 단건 저장 (Value 형태)
        super.set(singleKey, session);

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
     * 페이징용 Sorted Set에서 limit 개수만큼 watcherId 목록을 반환합니다.
     */
    public List<UUID> findWatcherIdsByContentId(UUID contentId, int limit, Double maxScore) {
        String sortedSetKey = String.format(CONTENT_WATCHERS_KEY, contentId);

        // reverseRangeByScore: Score가 maxScore 이하인 Member를 내림차순으로 가져옴 (최신순 정렬)
        Set<String> members = stringRedisTemplate.opsForZSet().reverseRangeByScore(sortedSetKey, 0.0, maxScore, 0, limit);

        return convertToUUIDList(members);
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
        Boolean isMember = stringRedisTemplate.opsForZSet().score(sortedSetKey, watcherId.toString()) != null;
        return isMember != null && isMember;
    }

    /**
     * 특정 콘텐츠의 세션 수를 반환합니다. (countByContentId와 동일)
     */
    public long count(UUID contentId) {
        String sortedSetKey = String.format(CONTENT_WATCHERS_KEY, contentId);
        Long size = stringRedisTemplate.opsForZSet().zCard(sortedSetKey);
        return (size != null) ? size : 0;
    }

    /**
     * Set<Object>를 List<UUID>로 변환합니다.
     */
    private List<UUID> convertToUUIDList(Set<String> members) {
        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }

        return members.stream()
                .map(UUID::fromString)
                .toList();
    }
}

