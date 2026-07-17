package com.codeit.team5.mopl.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisLoginSessionStore implements LoginSessionStore {

    private static final String SESSION_KEY_PREFIX =
            "mopl:auth:login-session:";

    private static final String SESSION_INDEX_KEY_PREFIX =
            "mopl:auth:login-session-index:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public UUID save(UUID userId, Instant expiresAt) {
        Duration ttl = calculateTtl(expiresAt);
        UUID sessionId = UUID.randomUUID();

        String sessionKey = sessionKey(userId, sessionId);
        String indexKey = sessionIndexKey(userId);

        // 세션 하나를 독립적인 키로 저장한다.
        redisTemplate.opsForValue().set(
                sessionKey,
                sessionId.toString(),
                ttl
        );

        // 사용자별 세션 인덱스에 만료 시각을 score로 저장한다.
        redisTemplate.opsForZSet().add(
                indexKey,
                sessionId.toString(),
                expiresAt.toEpochMilli()
        );

        return sessionId;
    }

    @Override
    public Optional<UUID> findCurrentSessionId(UUID userId) {
        removeExpiredIndexEntries(userId);

        String indexKey = sessionIndexKey(userId);

        while (true) {
            Set<String> sessionIds =
                    redisTemplate.opsForZSet()
                            .reverseRange(indexKey, 0, 0);

            if (sessionIds == null || sessionIds.isEmpty()) {
                return Optional.empty();
            }

            String sessionIdValue = sessionIds.iterator().next();
            UUID sessionId = UUID.fromString(sessionIdValue);

            if (redisTemplate.hasKey(sessionKey(userId, sessionId))) {
                return Optional.of(sessionId);
            }

            /*
             * 세션 키는 TTL로 이미 제거됐지만 인덱스 멤버가 남아 있는 경우
             * stale member를 제거하고 다음 세션을 조회한다.
             */
            redisTemplate.opsForZSet()
                    .remove(indexKey, sessionIdValue);
        }
    }

    @Override
    public Optional<UUID> extendCurrentSession(
            UUID userId,
            Instant expiresAt
    ) {
        Duration ttl = calculateTtl(expiresAt);

        Optional<UUID> currentSessionId =
                findCurrentSessionId(userId);

        if (currentSessionId.isEmpty()) {
            return Optional.empty();
        }

        UUID sessionId = currentSessionId.get();
        String sessionKey = sessionKey(userId, sessionId);

        Boolean extended = redisTemplate.expire(sessionKey, ttl);

        if (!Boolean.TRUE.equals(extended)) {
            /*
             * findCurrentSessionId() 이후 세션이 로그아웃 또는 만료되어
             * 사라진 경우 인덱스에서도 제거한다.
             */
            redisTemplate.opsForZSet().remove(
                    sessionIndexKey(userId),
                    sessionId.toString()
            );

            return Optional.empty();
        }

        redisTemplate.opsForZSet().add(
                sessionIndexKey(userId),
                sessionId.toString(),
                expiresAt.toEpochMilli()
        );

        return Optional.of(sessionId);
    }

    @Override
    public boolean isValid(UUID userId, UUID sessionId) {
        return redisTemplate.hasKey(sessionKey(userId, sessionId));
    }

    @Override
    public void deleteByUserId(UUID userId) {
        String indexKey = sessionIndexKey(userId);

        Set<String> sessionIds =
                redisTemplate.opsForZSet().range(indexKey, 0, -1);

        if (sessionIds != null && !sessionIds.isEmpty()) {
            List<String> sessionKeys = sessionIds.stream()
                    .map(UUID::fromString)
                    .map(sessionId -> sessionKey(userId, sessionId))
                    .toList();

            redisTemplate.delete(sessionKeys);
        }

        redisTemplate.delete(indexKey);
    }

    @Override
    public void deleteExpiredSessions() {
        /*
         * 개별 세션 키는 Redis TTL에 의해 자동 삭제된다.
         *
         * 사용자별 Sorted Set 인덱스의 만료 멤버는 사용자 세션을 조회할 때
         * removeExpiredIndexEntries()에서 지연 정리한다.
         */
    }

    private void removeExpiredIndexEntries(UUID userId) {
        redisTemplate.opsForZSet().removeRangeByScore(
                sessionIndexKey(userId),
                Double.NEGATIVE_INFINITY,
                Instant.now().toEpochMilli()
        );
    }

    private String sessionKey(UUID userId, UUID sessionId) {
        return SESSION_KEY_PREFIX
                + userId
                + ":"
                + sessionId;
    }

    private String sessionIndexKey(UUID userId) {
        return SESSION_INDEX_KEY_PREFIX + userId;
    }

    private Duration calculateTtl(Instant expiresAt) {
        Duration ttl = Duration.between(
                Instant.now(),
                expiresAt
        );

        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException(
                    "로그인 세션 만료 시각은 현재 시각보다 이후여야 합니다."
            );
        }

        return ttl;
    }
}
