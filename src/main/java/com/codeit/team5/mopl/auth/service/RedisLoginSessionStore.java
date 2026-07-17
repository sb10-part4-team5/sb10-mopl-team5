package com.codeit.team5.mopl.auth.service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisLoginSessionStore implements LoginSessionStore {

    private static final String SESSION_KEY_PREFIX =
            "mopl:auth:login-session:";

    private static final String SESSION_INDEX_KEY_PREFIX =
            "mopl:auth:login-session-index:";

    /*
     * 개별 세션 Key 저장과 사용자별 세션 인덱스 추가를
     * 하나의 Redis 원자적 연산으로 처리한다.
     *
     * KEYS[1] = 개별 세션 Key
     * KEYS[2] = 사용자별 세션 인덱스 Key
     *
     * ARGV[1] = sessionId
     * ARGV[2] = expiresAt epoch milliseconds
     */
    private static final DefaultRedisScript<Long> SAVE_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    redis.call(
                        'SET',
                        KEYS[1],
                        ARGV[1],
                        'PXAT',
                        ARGV[2]
                    )

                    redis.call(
                        'ZADD',
                        KEYS[2],
                        ARGV[2],
                        ARGV[1]
                    )

                    return 1
                    """,
                    Long.class
            );

    /*
     * 개별 세션 Key의 만료 시각과 Sorted Set의 score를
     * 하나의 Redis 원자적 연산으로 갱신한다.
     *
     * KEYS[1] = 개별 세션 Key
     * KEYS[2] = 사용자별 세션 인덱스 Key
     *
     * ARGV[1] = sessionId
     * ARGV[2] = 새로운 expiresAt epoch milliseconds
     *
     * 반환값:
     * 1 = 세션이 존재하여 연장 성공
     * 0 = 세션 Key가 존재하지 않음
     */
    private static final DefaultRedisScript<Long> EXTEND_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    if redis.call('EXISTS', KEYS[1]) == 0 then
                        redis.call(
                            'ZREM',
                            KEYS[2],
                            ARGV[1]
                        )

                        return 0
                    end

                    redis.call(
                        'PEXPIREAT',
                        KEYS[1],
                        ARGV[2]
                    )

                    redis.call(
                        'ZADD',
                        KEYS[2],
                        ARGV[2],
                        ARGV[1]
                    )

                    return 1
                    """,
                    Long.class
            );

    /*
     * 사용자별 세션 인덱스 조회, 개별 세션 Key 전체 삭제,
     * 인덱스 Key 삭제를 하나의 Redis 원자적 연산으로 수행한다.
     *
     * KEYS[1] = 사용자별 세션 인덱스 Key
     *
     * ARGV[1] = 개별 세션 Key prefix
     *
     * 반환값:
     * 삭제를 시도한 개별 세션 수
     */
    private static final DefaultRedisScript<Long> DELETE_BY_USER_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    local sessionIds =
                        redis.call('ZRANGE', KEYS[1], 0, -1)

                    for _, sessionId in ipairs(sessionIds) do
                        redis.call(
                            'DEL',
                            ARGV[1] .. sessionId
                        )
                    end

                    redis.call('DEL', KEYS[1])

                    return #sessionIds
                    """,
                    Long.class
            );

    private final StringRedisTemplate redisTemplate;

    @Override
    public UUID save(UUID userId, Instant expiresAt) {
        validateExpiresAt(expiresAt);

        UUID sessionId = UUID.randomUUID();

        String sessionKey = sessionKey(userId, sessionId);
        String indexKey = sessionIndexKey(userId);
        String expiresAtMillis = String.valueOf(expiresAt.toEpochMilli());

        Long result = redisTemplate.execute(
                SAVE_SCRIPT,
                List.of(sessionKey, indexKey),
                sessionId.toString(),
                expiresAtMillis
        );

        if (!Long.valueOf(1L).equals(result)) {
            throw new IllegalStateException(
                    "로그인 세션 저장에 실패했습니다."
            );
        }

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

            String sessionIdValue =
                    sessionIds.iterator().next();

            UUID sessionId =
                    parseSessionId(
                            indexKey,
                            sessionIdValue
                    );

            if (Boolean.TRUE.equals(
                    redisTemplate.hasKey(
                            sessionKey(userId, sessionId)
                    )
            )) {
                return Optional.of(sessionId);
            }

            redisTemplate.opsForZSet().remove(
                    indexKey,
                    sessionIdValue
            );
        }
    }

    @Override
    public Optional<UUID> extendCurrentSession(
            UUID userId,
            Instant expiresAt
    ) {
        validateExpiresAt(expiresAt);

        Optional<UUID> currentSessionId =
                findCurrentSessionId(userId);

        if (currentSessionId.isEmpty()) {
            return Optional.empty();
        }

        UUID sessionId = currentSessionId.get();

        Long result = redisTemplate.execute(
                EXTEND_SCRIPT,
                List.of(
                        sessionKey(userId, sessionId),
                        sessionIndexKey(userId)
                ),
                sessionId.toString(),
                String.valueOf(expiresAt.toEpochMilli())
        );

        if (!Long.valueOf(1L).equals(result)) {
            return Optional.empty();
        }

        return Optional.of(sessionId);
    }

    @Override
    public boolean isValid(
            UUID userId,
            UUID sessionId
    ) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(
                        sessionKey(userId, sessionId)
                )
        );
    }

    @Override
    public void deleteByUserId(UUID userId) {
        String indexKey = sessionIndexKey(userId);

        redisTemplate.execute(
                DELETE_BY_USER_SCRIPT,
                Collections.singletonList(indexKey),
                sessionKeyPrefix(userId)
        );
    }

    @Override
    public void deleteExpiredSessions() {
        /*
         * 개별 세션 Key는 Redis TTL에 의해 자동으로 제거된다.
         *
         * 사용자별 Sorted Set의 만료 member는
         * findCurrentSessionId() 호출 시 지연 정리한다.
         */
    }

    private void removeExpiredIndexEntries(UUID userId) {
        redisTemplate.opsForZSet().removeRangeByScore(
                sessionIndexKey(userId),
                Double.NEGATIVE_INFINITY,
                Instant.now().toEpochMilli()
        );
    }

    private UUID parseSessionId(
            String indexKey,
            String sessionIdValue
    ) {
        try {
            return UUID.fromString(sessionIdValue);
        } catch (IllegalArgumentException exception) {
            redisTemplate.opsForZSet().remove(
                    indexKey,
                    sessionIdValue
            );

            throw new IllegalStateException(
                    "유효하지 않은 로그인 세션 식별자가 저장되어 있습니다.",
                    exception
            );
        }
    }

    private String sessionKey(
            UUID userId,
            UUID sessionId
    ) {
        return sessionKeyPrefix(userId) + sessionId;
    }

    private String sessionKeyPrefix(UUID userId) {
        return SESSION_KEY_PREFIX
                + userId
                + ":";
    }

    private String sessionIndexKey(UUID userId) {
        return SESSION_INDEX_KEY_PREFIX + userId;
    }

    private void validateExpiresAt(Instant expiresAt) {
        if (!expiresAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException(
                    "로그인 세션 만료 시각은 현재 시각보다 이후여야 합니다."
            );
        }
    }
}
