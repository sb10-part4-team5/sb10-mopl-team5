package com.codeit.team5.mopl.auth.service;

import com.codeit.team5.mopl.auth.exception.RefreshTokenSaveException;
import com.codeit.team5.mopl.auth.support.RefreshTokenHasher;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "mopl:auth:refresh-tokens:";

    private static final DefaultRedisScript<Long> SAVE_SCRIPT =
            createScript("redis/refresh-token/save.lua");

    private static final DefaultRedisScript<Long> EXISTS_VALID_SCRIPT =
            createScript("redis/refresh-token/exists-valid.lua");

    private static final DefaultRedisScript<Long> ROTATE_IF_VALID_SCRIPT =
            createScript("redis/refresh-token/rotate-if-valid.lua");

    private final StringRedisTemplate redisTemplate;
    private final RefreshTokenHasher refreshTokenHasher;

    @Override
    public void save(
            UUID userId,
            String rawToken,
            Instant expiresAt
    ) {
        UUID requiredUserId =
                Objects.requireNonNull(userId, "userId must not be null");
        String requiredRawToken =
                Objects.requireNonNull(rawToken, "rawToken must not be null");
        Instant requiredExpiresAt =
                Objects.requireNonNull(expiresAt, "expiresAt must not be null");

        Instant now = Instant.now();

        if (!requiredExpiresAt.isAfter(now)) {
            throw new IllegalArgumentException(
                    "expiresAt must be later than the current time"
            );
        }

        String key = createKey(requiredUserId);
        String tokenHash = refreshTokenHasher.hash(requiredRawToken);

        try {
            Long result = redisTemplate.execute(
                    SAVE_SCRIPT,
                    List.of(key),
                    Long.toString(now.toEpochMilli()),
                    tokenHash,
                    Long.toString(requiredExpiresAt.toEpochMilli())
            );

            if (!isSuccess(result)) {
                throw new RefreshTokenSaveException(
                        new IllegalStateException(
                                "Refresh token expiration time has already passed"
                        )
                );
            }
        } catch (DataAccessException e) {
            throw new RefreshTokenSaveException(e);
        }
    }

    @Override
    public boolean existsValidToken(
            UUID userId,
            String rawToken
    ) {
        UUID requiredUserId =
                Objects.requireNonNull(userId, "userId must not be null");
        String requiredRawToken =
                Objects.requireNonNull(rawToken, "rawToken must not be null");

        Instant now = Instant.now();
        String key = createKey(requiredUserId);
        String tokenHash = refreshTokenHasher.hash(requiredRawToken);

        Long result = redisTemplate.execute(
                EXISTS_VALID_SCRIPT,
                List.of(key),
                Long.toString(now.toEpochMilli()),
                tokenHash
        );

        return isSuccess(result);
    }

    @Override
    public boolean rotateIfValid(
            UUID userId,
            String oldToken,
            String newToken,
            Instant expiresAt
    ) {
        UUID requiredUserId =
                Objects.requireNonNull(userId, "userId must not be null");
        String requiredOldToken =
                Objects.requireNonNull(oldToken, "oldToken must not be null");
        String requiredNewToken =
                Objects.requireNonNull(newToken, "newToken must not be null");
        Instant requiredExpiresAt =
                Objects.requireNonNull(expiresAt, "expiresAt must not be null");

        Instant now = Instant.now();

        if (!requiredExpiresAt.isAfter(now)) {
            return false;
        }

        String key = createKey(requiredUserId);
        String oldTokenHash = refreshTokenHasher.hash(requiredOldToken);
        String newTokenHash = refreshTokenHasher.hash(requiredNewToken);

        Long result = redisTemplate.execute(
                ROTATE_IF_VALID_SCRIPT,
                List.of(key),
                Long.toString(now.toEpochMilli()),
                oldTokenHash,
                newTokenHash,
                Long.toString(requiredExpiresAt.toEpochMilli())
        );

        return isSuccess(result);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        UUID requiredUserId =
                Objects.requireNonNull(userId, "userId must not be null");

        redisTemplate.delete(createKey(requiredUserId));
    }

    @Override
    public void deleteExpiredTokens() {
        /*
         * 개별 토큰의 만료 시각은 Sorted Set score로 관리한다.
         *
         * save, existsValidToken, rotateIfValid 호출 시
         * ZREMRANGEBYSCORE를 통해 만료된 member를 지연 정리한다.
         *
         * 사용자별 키는 가장 늦게 만료되는 토큰의 expiresAt을 기준으로
         * PEXPIREAT이 설정된다. 모든 토큰이 만료되면 Redis가 해당 키를
         * 자동으로 제거하므로 전체 키를 SCAN하지 않는다.
         */
    }

    private static DefaultRedisScript<Long> createScript(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(Long.class);
        return script;
    }

    private String createKey(UUID userId) {
        return KEY_PREFIX + userId;
    }

    private boolean isSuccess(Long result) {
        return Long.valueOf(1L).equals(result);
    }
}
