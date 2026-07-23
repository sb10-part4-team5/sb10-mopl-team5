package com.codeit.team5.mopl.auth.service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisLoginSessionStore implements LoginSessionStore {

    private static final String SESSION_KEY_PREFIX =
            "mopl:auth:login-session:";

    private static final String SESSION_INDEX_KEY_PREFIX =
            "mopl:auth:login-session-index:";

    private static final RedisScript<Long> SAVE_SCRIPT =
            RedisScript.of(
                    new ClassPathResource(
                            "redis/login-session/save.lua"
                    ),
                    Long.class
            );

    private static final RedisScript<Long> EXTEND_SCRIPT =
            RedisScript.of(
                    new ClassPathResource(
                            "redis/login-session/extend.lua"
                    ),
                    Long.class
            );

    private static final RedisScript<Long> DELETE_BY_USER_SCRIPT =
            RedisScript.of(
                    new ClassPathResource(
                            "redis/login-session/delete-by-user.lua"
                    ),
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
         * 개별 세션 Key는 자체 TTL에 의해 자동으로 제거된다.
         *
         * 사용자별 Sorted Set 인덱스 Key도 Lua Script에서
         * 해당 사용자의 가장 늦은 세션 만료 시각에 맞춰 TTL을 설정하므로,
         * 모든 세션이 만료되면 자동으로 제거된다.
         *
         * 조회 시에는 removeExpiredIndexEntries()를 통해
         * 아직 인덱스에 남아 있는 만료 member를 추가로 지연 정리한다.
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
