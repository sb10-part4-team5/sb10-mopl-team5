package com.codeit.team5.mopl.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.auth.exception.RefreshTokenStoreException;
import com.codeit.team5.mopl.auth.support.RefreshTokenHasher;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@ExtendWith(MockitoExtension.class)
class RedisRefreshTokenStoreTest {

    private static final String KEY_PREFIX = "mopl:auth:refresh-tokens:";
    private static final String RAW_TOKEN = "raw-refresh-token";
    private static final String TOKEN_HASH = "hashed-refresh-token";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private RefreshTokenHasher refreshTokenHasher;

    private RedisRefreshTokenStore refreshTokenStore;

    @Autowired
    private StringRedisTemplate integrationRedisTemplate;

    @Autowired
    private RefreshTokenHasher integrationRefreshTokenHasher;

    private final Set<UUID> integrationUserIds = new HashSet<>();

    @BeforeEach
    void setUpUnitTarget() {
        refreshTokenStore = new RedisRefreshTokenStore(
                redisTemplate,
                refreshTokenHasher
        );
    }

    @AfterEach
    void cleanUpIntegrationKeys() {
        integrationUserIds.stream()
                .map(RedisRefreshTokenStoreTest::createKey)
                .forEach(integrationRedisTemplate::delete);
        integrationUserIds.clear();
    }

    @Test
    @DisplayName("리프레시 토큰을 해시해 올바른 인자로 저장 Script를 실행한다")
    void save_validToken_hashesTokenAndExecutesScript() {
        // Given
        UUID userId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(600);
        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        stubScriptResult(1L);
        long before = Instant.now().toEpochMilli();

        // When
        refreshTokenStore.save(userId, RAW_TOKEN, expiresAt);
        long after = Instant.now().toEpochMilli();

        // Then
        ScriptExecution execution = captureExecution(3);
        assertScript(execution, createKey(userId));
        assertCurrentTime(execution.arguments().get(0), before, after);
        assertThat(execution.arguments())
                .containsExactly(
                        execution.arguments().get(0),
                        TOKEN_HASH,
                        Long.toString(expiresAt.toEpochMilli())
                );
        verify(refreshTokenHasher).hash(RAW_TOKEN);
    }

    @Test
    @DisplayName("저장 Script가 0을 반환하면 RefreshTokenStoreException을 던진다")
    void save_scriptReturnsZero_throwsRefreshTokenStoreException() {
        // Given
        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        stubScriptResult(0L);

        // When & Then
        RefreshTokenStoreException exception = catchThrowableOfType(
                () -> refreshTokenStore.save(
                        UUID.randomUUID(),
                        RAW_TOKEN,
                        Instant.now().plusSeconds(600)
                ),
                RefreshTokenStoreException.class
        );
        assertThat(exception.getMessage())
                .isEqualTo("리프레시 토큰 저장에 실패했습니다.");
        assertThat(exception.getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("Redis 저장 중 DataAccessException이 발생하면 RefreshTokenStoreException으로 변환한다")
    void save_redisFailure_throwsRefreshTokenStoreException() {
        // Given
        DataAccessResourceFailureException redisException =
                new DataAccessResourceFailureException("Redis unavailable");
        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(redisTemplate.execute(
                any(RedisScript.class),
                any(List.class),
                any(),
                any(),
                any()
        )).thenThrow(redisException);

        // When & Then
        RefreshTokenStoreException exception = catchThrowableOfType(
                () -> refreshTokenStore.save(
                        UUID.randomUUID(),
                        RAW_TOKEN,
                        Instant.now().plusSeconds(600)
                ),
                RefreshTokenStoreException.class
        );
        assertThat(exception.getMessage())
                .isEqualTo("리프레시 토큰 저장소 처리 중 오류가 발생했습니다.");
        assertThat(exception.getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(exception.getCause()).isSameAs(redisException);
    }

    @Test
    @DisplayName("미래가 아닌 만료 시각이면 저장하지 않는다")
    void save_invalidExpiration_throwsExceptionWithoutDependencies() {
        // Given
        Instant expiredAt = Instant.parse("2000-01-01T00:00:00Z");

        // When & Then
        assertThatThrownBy(() -> refreshTokenStore.save(
                UUID.randomUUID(),
                RAW_TOKEN,
                expiredAt
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("expiresAt must be later than the current time");
        verifyNoInteractions(refreshTokenHasher, redisTemplate);
    }

    @Test
    @DisplayName("사용자 식별자가 null이면 명확한 메시지로 저장에 실패한다")
    void save_nullUserId_throwsNullPointerException() {
        // Given
        Instant expiresAt = Instant.now().plusSeconds(600);

        // When & Then
        assertThatNullPointerException()
                .isThrownBy(() -> refreshTokenStore.save(null, RAW_TOKEN, expiresAt))
                .withMessage("userId must not be null");
    }

    @Test
    @DisplayName("리프레시 토큰 원문이 null이면 명확한 메시지로 저장에 실패한다")
    void save_nullRawToken_throwsNullPointerException() {
        // Given
        UUID userId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(600);

        // When & Then
        assertThatNullPointerException()
                .isThrownBy(() -> refreshTokenStore.save(userId, null, expiresAt))
                .withMessage("rawToken must not be null");
    }

    @Test
    @DisplayName("만료 시각이 null이면 명확한 메시지로 저장에 실패한다")
    void save_nullExpiresAt_throwsNullPointerException() {
        // Given
        UUID userId = UUID.randomUUID();

        // When & Then
        assertThatNullPointerException()
                .isThrownBy(() -> refreshTokenStore.save(userId, RAW_TOKEN, null))
                .withMessage("expiresAt must not be null");
    }

    @Test
    @DisplayName("유효 토큰 조회 Script가 1을 반환하면 true를 반환한다")
    void existsValidToken_scriptReturnsOne_returnsTrue() {
        // Given
        UUID userId = UUID.randomUUID();
        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        stubScriptResult(1L);
        long before = Instant.now().toEpochMilli();

        // When
        boolean result = refreshTokenStore.existsValidToken(userId, RAW_TOKEN);
        long after = Instant.now().toEpochMilli();

        // Then
        assertThat(result).isTrue();
        ScriptExecution execution = captureExecution(2);
        assertScript(execution, createKey(userId));
        assertCurrentTime(execution.arguments().get(0), before, after);
        assertThat(execution.arguments().get(1)).isEqualTo(TOKEN_HASH);
        verify(refreshTokenHasher).hash(RAW_TOKEN);
    }

    @Test
    @DisplayName("유효 토큰 조회 Script가 0을 반환하면 false를 반환한다")
    void existsValidToken_scriptReturnsZero_returnsFalse() {
        // Given
        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        stubScriptResult(0L);

        // When
        boolean result = refreshTokenStore.existsValidToken(UUID.randomUUID(), RAW_TOKEN);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("유효 토큰 조회 Script가 null을 반환하면 false를 반환한다")
    void existsValidToken_scriptReturnsNull_returnsFalse() {
        // Given
        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        stubScriptResult(null);

        // When
        boolean result = refreshTokenStore.existsValidToken(UUID.randomUUID(), RAW_TOKEN);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("유효 토큰 조회 중 DataAccessException이 발생하면 RefreshTokenStoreException으로 변환한다")
    void existsValidToken_redisFailure_throwsRefreshTokenStoreException() {
        // Given
        DataAccessResourceFailureException redisException =
                new DataAccessResourceFailureException("Redis unavailable");
        when(refreshTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(redisTemplate.execute(
                any(RedisScript.class),
                any(List.class),
                any(),
                any()
        )).thenThrow(redisException);

        // When & Then
        RefreshTokenStoreException exception = catchThrowableOfType(
                () -> refreshTokenStore.existsValidToken(UUID.randomUUID(), RAW_TOKEN),
                RefreshTokenStoreException.class
        );
        assertThat(exception.getMessage())
                .isEqualTo("리프레시 토큰 저장소 처리 중 오류가 발생했습니다.");
        assertThat(exception.getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(exception.getCause()).isSameAs(redisException);
    }

    @Test
    @DisplayName("사용자 식별자가 null이면 명확한 메시지로 유효 토큰 조회에 실패한다")
    void existsValidToken_nullUserId_throwsNullPointerException() {
        // When & Then
        assertThatNullPointerException()
                .isThrownBy(() -> refreshTokenStore.existsValidToken(null, RAW_TOKEN))
                .withMessage("userId must not be null");
    }

    @Test
    @DisplayName("리프레시 토큰 원문이 null이면 명확한 메시지로 유효 토큰 조회에 실패한다")
    void existsValidToken_nullRawToken_throwsNullPointerException() {
        // Given
        UUID userId = UUID.randomUUID();

        // When & Then
        assertThatNullPointerException()
                .isThrownBy(() -> refreshTokenStore.existsValidToken(userId, null))
                .withMessage("rawToken must not be null");
    }

    @Test
    @DisplayName("유효한 기존 토큰을 새 토큰으로 회전하고 true를 반환한다")
    void rotateIfValid_scriptReturnsOne_hashesInOrderAndReturnsTrue() {
        // Given
        UUID userId = UUID.randomUUID();
        String oldToken = "old-refresh-token";
        String newToken = "new-refresh-token";
        String oldTokenHash = "old-hashed-refresh-token";
        String newTokenHash = "new-hashed-refresh-token";
        Instant expiresAt = Instant.now().plusSeconds(600);
        when(refreshTokenHasher.hash(oldToken)).thenReturn(oldTokenHash);
        when(refreshTokenHasher.hash(newToken)).thenReturn(newTokenHash);
        stubScriptResult(1L);
        long before = Instant.now().toEpochMilli();

        // When
        boolean result = refreshTokenStore.rotateIfValid(
                userId,
                oldToken,
                newToken,
                expiresAt
        );
        long after = Instant.now().toEpochMilli();

        // Then
        assertThat(result).isTrue();
        ScriptExecution execution = captureExecution(4);
        assertScript(execution, createKey(userId));
        assertCurrentTime(execution.arguments().get(0), before, after);
        assertThat(execution.arguments())
                .containsExactly(
                        execution.arguments().get(0),
                        oldTokenHash,
                        newTokenHash,
                        Long.toString(expiresAt.toEpochMilli())
                );
        org.mockito.InOrder order = org.mockito.Mockito.inOrder(refreshTokenHasher);
        order.verify(refreshTokenHasher).hash(oldToken);
        order.verify(refreshTokenHasher).hash(newToken);
    }

    @Test
    @DisplayName("토큰 회전 Script가 0을 반환하면 false를 반환한다")
    void rotateIfValid_scriptReturnsZero_returnsFalse() {
        // Given
        stubRotationHashes();
        stubScriptResult(0L);

        // When
        boolean result = rotateWithValidArguments();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("토큰 회전 Script가 null을 반환하면 false를 반환한다")
    void rotateIfValid_scriptReturnsNull_returnsFalse() {
        // Given
        stubRotationHashes();
        stubScriptResult(null);

        // When
        boolean result = rotateWithValidArguments();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("새 만료 시각이 미래가 아니면 의존 객체 호출 없이 false를 반환한다")
    void rotateIfValid_invalidExpiration_returnsFalseWithoutDependencies() {
        // Given
        Instant expiredAt = Instant.parse("2000-01-01T00:00:00Z");

        // When
        boolean result = refreshTokenStore.rotateIfValid(
                UUID.randomUUID(),
                "old-token",
                "new-token",
                expiredAt
        );

        // Then
        assertThat(result).isFalse();
        verifyNoInteractions(refreshTokenHasher, redisTemplate);
    }

    @Test
    @DisplayName("토큰 회전 중 DataAccessException이 발생하면 RefreshTokenStoreException으로 변환한다")
    void rotateIfValid_redisFailure_throwsRefreshTokenStoreException() {
        // Given
        DataAccessResourceFailureException redisException =
                new DataAccessResourceFailureException("Redis unavailable");
        stubRotationHashes();
        when(redisTemplate.execute(
                any(RedisScript.class),
                any(List.class),
                any(),
                any(),
                any(),
                any()
        )).thenThrow(redisException);

        // When & Then
        RefreshTokenStoreException exception = catchThrowableOfType(
                this::rotateWithValidArguments,
                RefreshTokenStoreException.class
        );
        assertThat(exception.getMessage())
                .isEqualTo("리프레시 토큰 저장소 처리 중 오류가 발생했습니다.");
        assertThat(exception.getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(exception.getCause()).isSameAs(redisException);
    }

    @Test
    @DisplayName("사용자 식별자가 null이면 명확한 메시지로 토큰 회전에 실패한다")
    void rotateIfValid_nullUserId_throwsNullPointerException() {
        // When & Then
        assertThatNullPointerException()
                .isThrownBy(() -> refreshTokenStore.rotateIfValid(
                        null,
                        "old-token",
                        "new-token",
                        Instant.now().plusSeconds(600)
                ))
                .withMessage("userId must not be null");
    }

    @Test
    @DisplayName("기존 토큰이 null이면 명확한 메시지로 토큰 회전에 실패한다")
    void rotateIfValid_nullOldToken_throwsNullPointerException() {
        // When & Then
        assertThatNullPointerException()
                .isThrownBy(() -> refreshTokenStore.rotateIfValid(
                        UUID.randomUUID(),
                        null,
                        "new-token",
                        Instant.now().plusSeconds(600)
                ))
                .withMessage("oldToken must not be null");
    }

    @Test
    @DisplayName("새 토큰이 null이면 명확한 메시지로 토큰 회전에 실패한다")
    void rotateIfValid_nullNewToken_throwsNullPointerException() {
        // When & Then
        assertThatNullPointerException()
                .isThrownBy(() -> refreshTokenStore.rotateIfValid(
                        UUID.randomUUID(),
                        "old-token",
                        null,
                        Instant.now().plusSeconds(600)
                ))
                .withMessage("newToken must not be null");
    }

    @Test
    @DisplayName("새 만료 시각이 null이면 명확한 메시지로 토큰 회전에 실패한다")
    void rotateIfValid_nullExpiresAt_throwsNullPointerException() {
        // When & Then
        assertThatNullPointerException()
                .isThrownBy(() -> refreshTokenStore.rotateIfValid(
                        UUID.randomUUID(),
                        "old-token",
                        "new-token",
                        null
                ))
                .withMessage("expiresAt must not be null");
    }

    @Test
    @DisplayName("사용자 식별자로 생성한 Redis Key를 삭제한다")
    void deleteByUserId_validUserId_deletesUserKey() {
        // Given
        UUID userId = UUID.randomUUID();

        // When
        refreshTokenStore.deleteByUserId(userId);

        // Then
        verify(redisTemplate).delete(createKey(userId));
    }

    @Test
    @DisplayName("사용자 식별자가 null이면 명확한 메시지로 삭제에 실패한다")
    void deleteByUserId_nullUserId_throwsNullPointerException() {
        // When & Then
        assertThatNullPointerException()
                .isThrownBy(() -> refreshTokenStore.deleteByUserId(null))
                .withMessage("userId must not be null");
        verify(redisTemplate, never()).delete(any(String.class));
    }

    @Test
    @DisplayName("만료 토큰 정리는 Redis에 맡기고 별도 작업을 하지 않는다")
    void deleteExpiredTokens_called_doesNotRequestRedisOperation() {
        // When & Then
        assertThatCode(refreshTokenStore::deleteExpiredTokens)
                .doesNotThrowAnyException();
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("실제 Redis에서 새 토큰 저장 시 기존 토큰을 제거하고 하나만 유지한다")
    void save_existingToken_replacesWithSingleActiveTokenInRedis() {
        // Given
        UUID userId = integrationUserId();
        String firstToken = "first-integration-token";
        String secondToken = "second-integration-token";
        Instant expiresAt = Instant.now().plusSeconds(600);
        RedisRefreshTokenStore integrationStore = integrationStore();

        // When
        integrationStore.save(userId, firstToken, expiresAt);
        boolean firstInitiallyValid =
                integrationStore.existsValidToken(userId, firstToken);
        integrationStore.save(userId, secondToken, expiresAt);

        // Then
        assertThat(firstInitiallyValid).isTrue();
        assertThat(integrationStore.existsValidToken(userId, firstToken)).isFalse();
        assertThat(integrationStore.existsValidToken(userId, secondToken)).isTrue();
        assertThat(integrationRedisTemplate.opsForZSet().zCard(createKey(userId)))
                .isEqualTo(1L);

        Long ttlMillis = integrationRedisTemplate.getExpire(
                createKey(userId),
                java.util.concurrent.TimeUnit.MILLISECONDS
        );
        long expectedTtl = expiresAt.toEpochMilli() - Instant.now().toEpochMilli();
        assertThat(ttlMillis).isNotNull();
        assertThat(ttlMillis).isPositive();
        assertThat(ttlMillis).isCloseTo(expectedTtl, org.assertj.core.data.Offset.offset(2000L));
    }

    @Test
    @DisplayName("실제 Redis에서 토큰 회전 시 기존 토큰을 제거하고 새 토큰 하나만 유지한다")
    void rotateIfValid_existingToken_replacesWithSingleNewTokenInRedis() {
        // Given
        UUID userId = integrationUserId();
        String oldToken = "old-integration-token";
        String newToken = "new-integration-token";
        RedisRefreshTokenStore integrationStore = integrationStore();
        integrationStore.save(userId, oldToken, Instant.now().plusSeconds(600));

        // When
        boolean result = integrationStore.rotateIfValid(
                userId,
                oldToken,
                newToken,
                Instant.now().plusSeconds(900)
        );

        // Then
        assertThat(result).isTrue();
        assertThat(integrationStore.existsValidToken(userId, oldToken)).isFalse();
        assertThat(integrationStore.existsValidToken(userId, newToken)).isTrue();
        assertThat(integrationRedisTemplate.opsForZSet().zCard(createKey(userId)))
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("실제 Redis에서 토큰 회전 시 예상하지 못한 다른 토큰도 모두 제거한다")
    void rotateIfValid_multipleExistingTokens_removesAllExceptNewTokenInRedis() {
        // Given
        UUID userId = integrationUserId();
        String oldToken = "old-direct-token";
        String otherToken = "other-direct-token";
        String newToken = "new-direct-token";
        String oldTokenHash = integrationRefreshTokenHasher.hash(oldToken);
        String otherTokenHash = integrationRefreshTokenHasher.hash(otherToken);
        String key = createKey(userId);
        double expiresAt = Instant.now().plusSeconds(600).toEpochMilli();
        integrationRedisTemplate.opsForZSet().add(key, oldTokenHash, expiresAt);
        integrationRedisTemplate.opsForZSet().add(key, otherTokenHash, expiresAt);

        // When
        boolean result = integrationStore().rotateIfValid(
                userId,
                oldToken,
                newToken,
                Instant.now().plusSeconds(900)
        );

        // Then
        String newTokenHash = integrationRefreshTokenHasher.hash(newToken);
        assertThat(result).isTrue();
        assertThat(integrationRedisTemplate.opsForZSet().zCard(key)).isEqualTo(1L);
        assertThat(integrationRedisTemplate.opsForZSet().score(key, oldTokenHash))
                .isNull();
        assertThat(integrationRedisTemplate.opsForZSet().score(key, otherTokenHash))
                .isNull();
        assertThat(integrationRedisTemplate.opsForZSet().score(key, newTokenHash))
                .isNotNull();
    }

    @Test
    @DisplayName("실제 Lua Script는 잘못된 만료 시각에서 기존 토큰을 보존한다")
    void luaScripts_invalidExpiration_rejectWithoutDeletingExistingToken() {
        // Given
        UUID userId = integrationUserId();
        String key = createKey(userId);
        String oldTokenHash = integrationRefreshTokenHasher.hash("boundary-old-token");
        long now = Instant.now().toEpochMilli();
        integrationRedisTemplate.opsForZSet().add(
                key,
                oldTokenHash,
                Instant.now().plusSeconds(600).toEpochMilli()
        );

        // When
        Long saveResult = integrationRedisTemplate.execute(
                integrationScript("redis/refresh-token/save.lua"),
                List.of(key),
                Long.toString(now),
                integrationRefreshTokenHasher.hash("expired-new-token"),
                Long.toString(now)
        );
        Long rotateResult = integrationRedisTemplate.execute(
                integrationScript("redis/refresh-token/rotate-if-valid.lua"),
                List.of(key),
                Long.toString(now),
                oldTokenHash,
                integrationRefreshTokenHasher.hash("invalid-rotation-token"),
                Long.toString(now)
        );

        // Then
        assertThat(saveResult).isZero();
        assertThat(rotateResult).isZero();
        assertThat(integrationRedisTemplate.opsForZSet().zCard(key)).isEqualTo(1L);
        assertThat(integrationRedisTemplate.opsForZSet().score(key, oldTokenHash))
                .isNotNull();
    }

    @Test
    @DisplayName("실제 Redis에서 만료 토큰 조회 시 false를 반환하고 member를 지연 정리한다")
    void existsValidToken_expiredMember_returnsFalseAndRemovesMemberInRedis() {
        // Given
        UUID userId = integrationUserId();
        String rawToken = "expired-integration-token";
        String tokenHash = integrationRefreshTokenHasher.hash(rawToken);
        String key = createKey(userId);
        integrationRedisTemplate.opsForZSet().add(
                key,
                tokenHash,
                Instant.now().minusSeconds(600).toEpochMilli()
        );

        // When
        boolean result = integrationStore().existsValidToken(userId, rawToken);

        // Then
        assertThat(result).isFalse();
        assertThat(integrationRedisTemplate.opsForZSet().zCard(key)).isZero();
    }

    private void stubScriptResult(Long result) {
        when(redisTemplate.execute(
                any(RedisScript.class),
                any(List.class),
                any(Object[].class)
        )).thenReturn(result);
    }

    private void stubRotationHashes() {
        when(refreshTokenHasher.hash("old-token")).thenReturn("old-token-hash");
        when(refreshTokenHasher.hash("new-token")).thenReturn("new-token-hash");
    }

    private boolean rotateWithValidArguments() {
        return refreshTokenStore.rotateIfValid(
                UUID.randomUUID(),
                "old-token",
                "new-token",
                Instant.now().plusSeconds(600)
        );
    }

    private RedisRefreshTokenStore integrationStore() {
        return new RedisRefreshTokenStore(
                integrationRedisTemplate,
                integrationRefreshTokenHasher
        );
    }

    private UUID integrationUserId() {
        UUID userId = UUID.randomUUID();
        integrationUserIds.add(userId);
        return userId;
    }

    private DefaultRedisScript<Long> integrationScript(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(Long.class);
        return script;
    }

    private ScriptExecution captureExecution(int argumentCount) {
        ArgumentCaptor<RedisScript<Long>> scriptCaptor = redisScriptCaptor();
        ArgumentCaptor<List<String>> keysCaptor = stringListCaptor();
        ArgumentCaptor<Object[]> argumentsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(redisTemplate).execute(
                scriptCaptor.capture(),
                keysCaptor.capture(),
                argumentsCaptor.capture()
        );
        assertThat(argumentsCaptor.getValue()).hasSize(argumentCount);
        return new ScriptExecution(
                scriptCaptor.getValue(),
                keysCaptor.getValue(),
                List.of(argumentsCaptor.getValue()).stream()
                        .map(String.class::cast)
                        .toList()
        );
    }

    private void assertScript(ScriptExecution execution, String expectedKey) {
        assertThat(execution.script()).isNotNull();
        assertThat(execution.script().getResultType()).isEqualTo(Long.class);
        assertThat(execution.keys()).containsExactly(expectedKey);
    }

    private void assertCurrentTime(String value, long before, long after) {
        assertThat(value).matches("\\d+");
        assertThat(Long.parseLong(value)).isBetween(before, after);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ArgumentCaptor<RedisScript<Long>> redisScriptCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(RedisScript.class);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ArgumentCaptor<List<String>> stringListCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
    }

    private static String createKey(UUID userId) {
        return KEY_PREFIX + userId;
    }

    private record ScriptExecution(
            RedisScript<Long> script,
            List<String> keys,
            List<String> arguments
    ) {
    }
}
