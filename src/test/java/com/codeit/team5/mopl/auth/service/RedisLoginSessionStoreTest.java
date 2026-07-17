package com.codeit.team5.mopl.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
class RedisLoginSessionStoreTest {

    private static final String SESSION_KEY_PREFIX = "mopl:auth:login-session:";
    private static final String INDEX_KEY_PREFIX = "mopl:auth:login-session-index:";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private RedisLoginSessionStore loginSessionStore;

    @Test
    @DisplayName("새 세션을 Lua Script로 저장하고 생성된 식별자를 반환한다")
    void save_newSession_executesScriptAndReturnsGeneratedId() {
        // Given
        UUID userId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(600);
        when(redisTemplate.execute(
                any(RedisScript.class),
                any(List.class),
                any(),
                any()
        )).thenReturn(1L);

        // When
        UUID result = loginSessionStore.save(userId, expiresAt);

        // Then
        ArgumentCaptor<RedisScript<Long>> scriptCaptor = redisScriptCaptor();
        verify(redisTemplate).execute(
                scriptCaptor.capture(),
                eq(List.of(sessionKey(userId, result), indexKey(userId))),
                eq(result.toString()),
                eq(String.valueOf(expiresAt.toEpochMilli()))
        );
        assertThat(scriptCaptor.getValue().getResultType()).isEqualTo(Long.class);
    }

    @Test
    @DisplayName("같은 사용자의 여러 세션을 서로 다른 키로 Lua Script에 전달한다")
    void save_sameUser_executesScriptWithIndependentSessionKeys() {
        // Given
        UUID userId = UUID.randomUUID();
        Instant firstExpiresAt = Instant.now().plusSeconds(600);
        Instant secondExpiresAt = Instant.now().plusSeconds(1200);
        when(redisTemplate.execute(
                any(RedisScript.class),
                any(List.class),
                any(),
                any()
        )).thenReturn(1L);

        // When
        UUID first = loginSessionStore.save(userId, firstExpiresAt);
        UUID second = loginSessionStore.save(userId, secondExpiresAt);

        // Then
        assertThat(first).isNotEqualTo(second);
        verify(redisTemplate).execute(
                any(RedisScript.class),
                eq(List.of(sessionKey(userId, first), indexKey(userId))),
                eq(first.toString()),
                eq(String.valueOf(firstExpiresAt.toEpochMilli()))
        );
        verify(redisTemplate).execute(
                any(RedisScript.class),
                eq(List.of(sessionKey(userId, second), indexKey(userId))),
                eq(second.toString()),
                eq(String.valueOf(secondExpiresAt.toEpochMilli()))
        );
        verify(redisTemplate, times(2)).execute(
                any(RedisScript.class),
                any(List.class),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("저장 Script 결과가 null이면 예외를 던진다")
    void save_nullScriptResult_throwsIllegalStateException() {
        // Given
        UUID userId = UUID.randomUUID();
        when(redisTemplate.execute(
                any(RedisScript.class),
                any(List.class),
                any(),
                any()
        )).thenReturn(null);

        // When & Then
        assertThatThrownBy(
                () -> loginSessionStore.save(userId, Instant.now().plusSeconds(600))
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("로그인 세션 저장에 실패했습니다.");
    }

    @Test
    @DisplayName("저장 Script 결과가 성공 코드가 아니면 예외를 던진다")
    void save_unsuccessfulScriptResult_throwsIllegalStateException() {
        // Given
        UUID userId = UUID.randomUUID();
        when(redisTemplate.execute(
                any(RedisScript.class),
                any(List.class),
                any(),
                any()
        )).thenReturn(0L);

        // When & Then
        assertThatThrownBy(
                () -> loginSessionStore.save(userId, Instant.now().plusSeconds(600))
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("로그인 세션 저장에 실패했습니다.");
    }

    @Test
    @DisplayName("과거 만료 시각으로 저장하면 예외가 발생하고 Redis에 접근하지 않는다")
    void save_pastExpiration_throwsExceptionWithoutRedisCalls() {
        // Given
        UUID userId = UUID.randomUUID();
        Instant expiredAt = Instant.now().minusSeconds(600);

        // When & Then
        assertThatThrownBy(() -> loginSessionStore.save(userId, expiredAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("로그인 세션 만료 시각은 현재 시각보다 이후여야 합니다.");
        verifyNoInteractions(redisTemplate, zSetOperations);
    }

    @Test
    @DisplayName("현재와 사실상 같은 만료 시각으로 저장하면 예외가 발생한다")
    void save_currentExpiration_throwsExceptionWithoutRedisCalls() {
        // Given
        UUID userId = UUID.randomUUID();
        Instant expiresAt = Instant.now();

        // When & Then
        assertThatThrownBy(() -> loginSessionStore.save(userId, expiresAt))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(redisTemplate, zSetOperations);
    }

    @Test
    @DisplayName("인덱스에 세션이 없으면 빈 Optional을 반환한다")
    void findCurrentSessionId_emptyIndex_returnsEmpty() {
        // Given
        UUID userId = UUID.randomUUID();
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange(indexKey(userId), 0, 0)).thenReturn(Set.of());

        // When
        Optional<UUID> result = loginSessionStore.findCurrentSessionId(userId);

        // Then
        assertThat(result).isEmpty();
        verifyExpiredIndexCleanup(userId);
    }

    @Test
    @DisplayName("최신 세션 키가 존재하면 해당 세션 식별자를 반환한다")
    void findCurrentSessionId_existingLatestSession_returnsSessionId() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange(indexKey(userId), 0, 0))
                .thenReturn(Set.of(sessionId.toString()));
        when(redisTemplate.hasKey(sessionKey(userId, sessionId))).thenReturn(true);

        // When
        Optional<UUID> result = loginSessionStore.findCurrentSessionId(userId);

        // Then
        assertThat(result).contains(sessionId);
        verifyExpiredIndexCleanup(userId);
        verify(redisTemplate).hasKey(sessionKey(userId, sessionId));
    }

    @Test
    @DisplayName("stale 최신 세션을 제거한 뒤 다음 유효한 세션을 반환한다")
    void findCurrentSessionId_staleThenValid_removesStaleAndReturnsValidSession() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID staleSessionId = UUID.randomUUID();
        UUID validSessionId = UUID.randomUUID();
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange(indexKey(userId), 0, 0))
                .thenReturn(Set.of(staleSessionId.toString()))
                .thenReturn(Set.of(validSessionId.toString()));
        when(redisTemplate.hasKey(sessionKey(userId, staleSessionId))).thenReturn(false);
        when(redisTemplate.hasKey(sessionKey(userId, validSessionId))).thenReturn(true);

        // When
        Optional<UUID> result = loginSessionStore.findCurrentSessionId(userId);

        // Then
        assertThat(result).contains(validSessionId);
        InOrder order = inOrder(zSetOperations, redisTemplate);
        order.verify(zSetOperations).removeRangeByScore(
                eq(indexKey(userId)),
                eq(Double.NEGATIVE_INFINITY),
                anyDouble()
        );
        order.verify(zSetOperations).reverseRange(indexKey(userId), 0, 0);
        order.verify(redisTemplate).hasKey(sessionKey(userId, staleSessionId));
        order.verify(zSetOperations).remove(indexKey(userId), staleSessionId.toString());
        order.verify(zSetOperations).reverseRange(indexKey(userId), 0, 0);
        order.verify(redisTemplate).hasKey(sessionKey(userId, validSessionId));
    }

    @Test
    @DisplayName("잘못된 UUID member를 제거하고 IllegalStateException을 던진다")
    void findCurrentSessionId_invalidUuid_removesMemberAndThrowsException() {
        // Given
        UUID userId = UUID.randomUUID();
        String invalidSessionId = "invalid-session-id";
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange(indexKey(userId), 0, 0))
                .thenReturn(Set.of(invalidSessionId));

        // When & Then
        assertThatThrownBy(() -> loginSessionStore.findCurrentSessionId(userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("유효하지 않은 로그인 세션 식별자가 저장되어 있습니다.")
                .hasCauseInstanceOf(IllegalArgumentException.class);
        verify(zSetOperations).remove(indexKey(userId), invalidSessionId);
        verify(redisTemplate, never()).hasKey(any());
    }

    @Test
    @DisplayName("hasKey가 null이면 stale 세션으로 제거하고 다음 조회 결과를 반환한다")
    void findCurrentSessionId_nullHasKey_removesStaleAndReturnsEmpty() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange(indexKey(userId), 0, 0))
                .thenReturn(Set.of(sessionId.toString()))
                .thenReturn(Set.of());
        when(redisTemplate.hasKey(sessionKey(userId, sessionId))).thenReturn(null);

        // When
        Optional<UUID> result = loginSessionStore.findCurrentSessionId(userId);

        // Then
        assertThat(result).isEmpty();
        verify(zSetOperations).remove(indexKey(userId), sessionId.toString());
        verify(zSetOperations, times(2)).reverseRange(indexKey(userId), 0, 0);
    }

    @Test
    @DisplayName("현재 세션을 Lua Script로 연장하고 세션 식별자를 반환한다")
    void extendCurrentSession_existingSession_executesScriptAndReturnsSessionId() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(600);
        stubCurrentSession(userId, sessionId);
        when(redisTemplate.execute(
                any(RedisScript.class),
                any(List.class),
                any(),
                any()
        )).thenReturn(1L);

        // When
        Optional<UUID> result = loginSessionStore.extendCurrentSession(userId, expiresAt);

        // Then
        assertThat(result).contains(sessionId);
        ArgumentCaptor<RedisScript<Long>> scriptCaptor = redisScriptCaptor();
        verify(redisTemplate).execute(
                scriptCaptor.capture(),
                eq(List.of(sessionKey(userId, sessionId), indexKey(userId))),
                eq(sessionId.toString()),
                eq(String.valueOf(expiresAt.toEpochMilli()))
        );
        assertThat(scriptCaptor.getValue().getResultType()).isEqualTo(Long.class);
    }

    @Test
    @DisplayName("현재 세션이 없으면 연장 Script를 실행하지 않고 빈 Optional을 반환한다")
    void extendCurrentSession_missingSession_returnsEmptyWithoutScript() {
        // Given
        UUID userId = UUID.randomUUID();
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange(indexKey(userId), 0, 0)).thenReturn(Set.of());

        // When
        Optional<UUID> result =
                loginSessionStore.extendCurrentSession(userId, Instant.now().plusSeconds(600));

        // Then
        assertThat(result).isEmpty();
        verify(redisTemplate, never()).execute(
                any(RedisScript.class),
                any(List.class),
                any()
        );
    }

    @Test
    @DisplayName("연장 Script 결과가 null이면 빈 Optional을 반환한다")
    void extendCurrentSession_nullScriptResult_returnsEmpty() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        stubCurrentSession(userId, sessionId);
        when(redisTemplate.execute(
                any(RedisScript.class),
                any(List.class),
                any(),
                any()
        )).thenReturn(null);

        // When
        Optional<UUID> result =
                loginSessionStore.extendCurrentSession(userId, Instant.now().plusSeconds(600));

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("연장 Script 결과가 성공 코드가 아니면 빈 Optional을 반환한다")
    void extendCurrentSession_unsuccessfulScriptResult_returnsEmpty() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        stubCurrentSession(userId, sessionId);
        when(redisTemplate.execute(
                any(RedisScript.class),
                any(List.class),
                any(),
                any()
        )).thenReturn(0L);

        // When
        Optional<UUID> result =
                loginSessionStore.extendCurrentSession(userId, Instant.now().plusSeconds(600));

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("과거 만료 시각으로 연장하면 예외가 발생하고 Redis에 접근하지 않는다")
    void extendCurrentSession_pastExpiration_throwsExceptionWithoutRedisCalls() {
        // Given
        UUID userId = UUID.randomUUID();

        // When & Then
        assertThatThrownBy(
                () -> loginSessionStore.extendCurrentSession(
                        userId,
                        Instant.now().minusSeconds(600)
                )
        ).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(redisTemplate, zSetOperations);
    }

    @Test
    @DisplayName("세션 키가 존재하면 유효하다")
    void isValid_existingSessionKey_returnsTrue() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(redisTemplate.hasKey(sessionKey(userId, sessionId))).thenReturn(true);

        // When
        boolean result = loginSessionStore.isValid(userId, sessionId);

        // Then
        assertThat(result).isTrue();
        verify(redisTemplate).hasKey(sessionKey(userId, sessionId));
    }

    @Test
    @DisplayName("세션 키가 존재하지 않으면 유효하지 않다")
    void isValid_missingSessionKey_returnsFalse() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(redisTemplate.hasKey(sessionKey(userId, sessionId))).thenReturn(false);

        // When
        boolean result = loginSessionStore.isValid(userId, sessionId);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("세션 키 존재 여부가 null이면 유효하지 않다")
    void isValid_nullHasKey_returnsFalse() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(redisTemplate.hasKey(sessionKey(userId, sessionId))).thenReturn(null);

        // When
        boolean result = loginSessionStore.isValid(userId, sessionId);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("사용자 또는 세션 식별자가 다르면 각각 다른 세션 키를 조회한다")
    void isValid_differentIdentifiers_usesDifferentSessionKeys() {
        // Given
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();
        UUID firstSessionId = UUID.randomUUID();
        UUID secondSessionId = UUID.randomUUID();

        // When
        loginSessionStore.isValid(firstUserId, firstSessionId);
        loginSessionStore.isValid(secondUserId, firstSessionId);
        loginSessionStore.isValid(firstUserId, secondSessionId);

        // Then
        verify(redisTemplate).hasKey(sessionKey(firstUserId, firstSessionId));
        verify(redisTemplate).hasKey(sessionKey(secondUserId, firstSessionId));
        verify(redisTemplate).hasKey(sessionKey(firstUserId, secondSessionId));
    }

    @Test
    @DisplayName("사용자 세션을 Lua Script 한 번으로 삭제한다")
    void deleteByUserId_called_executesScriptOnce() {
        // Given
        UUID userId = UUID.randomUUID();

        // When
        loginSessionStore.deleteByUserId(userId);

        // Then
        ArgumentCaptor<RedisScript<Long>> scriptCaptor = redisScriptCaptor();
        verify(redisTemplate).execute(
                scriptCaptor.capture(),
                eq(List.of(indexKey(userId))),
                eq(sessionKeyPrefix(userId))
        );
        assertThat(scriptCaptor.getValue().getResultType()).isEqualTo(Long.class);
        verify(redisTemplate, times(1)).execute(
                any(RedisScript.class),
                any(List.class),
                any()
        );
    }

    private void stubCurrentSession(UUID userId, UUID sessionId) {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange(indexKey(userId), 0, 0))
                .thenReturn(Set.of(sessionId.toString()));
        when(redisTemplate.hasKey(sessionKey(userId, sessionId))).thenReturn(true);
    }

    private void verifyExpiredIndexCleanup(UUID userId) {
        verify(zSetOperations).removeRangeByScore(
                eq(indexKey(userId)),
                eq(Double.NEGATIVE_INFINITY),
                anyDouble()
        );
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ArgumentCaptor<RedisScript<Long>> redisScriptCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(RedisScript.class);
    }

    private static String sessionKey(UUID userId, UUID sessionId) {
        return sessionKeyPrefix(userId) + sessionId;
    }

    private static String sessionKeyPrefix(UUID userId) {
        return SESSION_KEY_PREFIX + userId + ":";
    }

    private static String indexKey(UUID userId) {
        return INDEX_KEY_PREFIX + userId;
    }
}
