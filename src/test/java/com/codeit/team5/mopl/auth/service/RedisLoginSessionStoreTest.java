package com.codeit.team5.mopl.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
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
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

@ExtendWith(MockitoExtension.class)
class RedisLoginSessionStoreTest {

    private static final String SESSION_KEY_PREFIX = "mopl:auth:login-session:";
    private static final String INDEX_KEY_PREFIX = "mopl:auth:login-session-index:";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private RedisLoginSessionStore loginSessionStore;

    @Test
    @DisplayName("새 세션을 세션별 키와 사용자 인덱스에 저장하고 생성된 식별자를 반환한다")
    void save_newSession_storesSessionAndReturnsGeneratedId() {
        // Given
        UUID userId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(600);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        // When
        UUID result = loginSessionStore.save(userId, expiresAt);

        // Then
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), ttlCaptor.capture());

        assertThat(result).isNotNull();
        assertThat(valueCaptor.getValue()).isEqualTo(result.toString());
        assertThat(keyCaptor.getValue()).isEqualTo(sessionKey(userId, result));
        assertThat(ttlCaptor.getValue()).isPositive();
        assertThat(ttlCaptor.getValue()).isLessThanOrEqualTo(Duration.ofSeconds(600));
        verify(zSetOperations).add(indexKey(userId), result.toString(), expiresAt.toEpochMilli());
    }

    @Test
    @DisplayName("같은 사용자의 여러 세션을 서로 다른 키로 저장하고 기존 세션을 삭제하지 않는다")
    void save_sameUser_storesMultipleIndependentSessionsWithoutDeleting() {
        // Given
        UUID userId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        // When
        UUID first = loginSessionStore.save(userId, Instant.now().plusSeconds(600));
        UUID second = loginSessionStore.save(userId, Instant.now().plusSeconds(1200));

        // Then
        assertThat(first).isNotEqualTo(second);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations, org.mockito.Mockito.times(2))
                .set(keyCaptor.capture(), org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(Duration.class));
        assertThat(keyCaptor.getAllValues())
                .containsExactly(sessionKey(userId, first), sessionKey(userId, second));
        verify(zSetOperations).add(
                org.mockito.ArgumentMatchers.eq(indexKey(userId)),
                org.mockito.ArgumentMatchers.eq(first.toString()),
                org.mockito.ArgumentMatchers.anyDouble());
        verify(zSetOperations).add(
                org.mockito.ArgumentMatchers.eq(indexKey(userId)),
                org.mockito.ArgumentMatchers.eq(second.toString()),
                org.mockito.ArgumentMatchers.anyDouble());
        verify(redisTemplate, never()).delete(org.mockito.ArgumentMatchers.anyString());
        verify(redisTemplate, never()).delete(org.mockito.ArgumentMatchers.<String>anyCollection());
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
        verifyNoInteractions(redisTemplate, valueOperations, zSetOperations);
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
        verifyNoInteractions(redisTemplate, valueOperations, zSetOperations);
    }

    @Test
    @DisplayName("만료 인덱스를 정리한 뒤 유효한 현재 세션을 반환한다")
    void findCurrentSessionId_existingSession_returnsSessionId() {
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
        verify(zSetOperations).removeRangeByScore(
                org.mockito.ArgumentMatchers.eq(indexKey(userId)),
                org.mockito.ArgumentMatchers.eq(Double.NEGATIVE_INFINITY),
                org.mockito.ArgumentMatchers.anyDouble());
        verify(redisTemplate).hasKey(sessionKey(userId, sessionId));
    }

    @Test
    @DisplayName("인덱스에 세션이 없으면 빈 Optional을 반환한다")
    void findCurrentSessionId_missingSession_returnsEmpty() {
        // Given
        UUID userId = UUID.randomUUID();
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange(indexKey(userId), 0, 0)).thenReturn(Set.of());

        // When
        Optional<UUID> result = loginSessionStore.findCurrentSessionId(userId);

        // Then
        assertThat(result).isEmpty();
        verify(zSetOperations).removeRangeByScore(
                org.mockito.ArgumentMatchers.eq(indexKey(userId)),
                org.mockito.ArgumentMatchers.eq(Double.NEGATIVE_INFINITY),
                org.mockito.ArgumentMatchers.anyDouble());
    }

    @Test
    @DisplayName("stale 세션을 인덱스에서 제거한 뒤 다음 유효한 세션을 반환한다")
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
        verify(zSetOperations).remove(indexKey(userId), staleSessionId.toString());
        InOrder order = inOrder(zSetOperations, redisTemplate);
        order.verify(zSetOperations).removeRangeByScore(
                org.mockito.ArgumentMatchers.eq(indexKey(userId)),
                org.mockito.ArgumentMatchers.eq(Double.NEGATIVE_INFINITY),
                org.mockito.ArgumentMatchers.anyDouble());
        order.verify(zSetOperations).reverseRange(indexKey(userId), 0, 0);
        order.verify(redisTemplate).hasKey(sessionKey(userId, staleSessionId));
        order.verify(zSetOperations).remove(indexKey(userId), staleSessionId.toString());
        order.verify(zSetOperations).reverseRange(indexKey(userId), 0, 0);
        order.verify(redisTemplate).hasKey(sessionKey(userId, validSessionId));
    }

    @Test
    @DisplayName("현재 세션의 TTL과 인덱스 만료 시각을 연장하고 세션 식별자를 반환한다")
    void extendCurrentSession_existingSession_extendsTtlAndIndex() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(600);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange(indexKey(userId), 0, 0))
                .thenReturn(Set.of(sessionId.toString()));
        when(redisTemplate.hasKey(sessionKey(userId, sessionId))).thenReturn(true);
        when(redisTemplate.expire(
                org.mockito.ArgumentMatchers.eq(sessionKey(userId, sessionId)),
                org.mockito.ArgumentMatchers.any(Duration.class)))
                .thenReturn(true);

        // When
        Optional<UUID> result = loginSessionStore.extendCurrentSession(userId, expiresAt);

        // Then
        assertThat(result).contains(sessionId);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(redisTemplate).expire(
                org.mockito.ArgumentMatchers.eq(sessionKey(userId, sessionId)),
                ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isPositive();
        assertThat(ttlCaptor.getValue()).isLessThanOrEqualTo(Duration.ofSeconds(600));
        verify(zSetOperations).add(indexKey(userId), sessionId.toString(), expiresAt.toEpochMilli());
    }

    @Test
    @DisplayName("현재 세션이 없으면 연장하지 않고 빈 Optional을 반환한다")
    void extendCurrentSession_missingSession_returnsEmptyWithoutExpire() {
        // Given
        UUID userId = UUID.randomUUID();
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange(indexKey(userId), 0, 0)).thenReturn(Set.of());

        // When
        Optional<UUID> result =
                loginSessionStore.extendCurrentSession(userId, Instant.now().plusSeconds(600));

        // Then
        assertThat(result).isEmpty();
        verify(redisTemplate, never()).expire(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Duration.class));
    }

    @Test
    @DisplayName("조회 후 세션 키가 사라지면 stale 인덱스를 제거하고 빈 Optional을 반환한다")
    void extendCurrentSession_sessionDisappears_removesStaleIndexAndReturnsEmpty() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange(indexKey(userId), 0, 0))
                .thenReturn(Set.of(sessionId.toString()));
        when(redisTemplate.hasKey(sessionKey(userId, sessionId))).thenReturn(true);
        when(redisTemplate.expire(
                org.mockito.ArgumentMatchers.eq(sessionKey(userId, sessionId)),
                org.mockito.ArgumentMatchers.any(Duration.class)))
                .thenReturn(false);

        // When
        Optional<UUID> result =
                loginSessionStore.extendCurrentSession(userId, Instant.now().plusSeconds(600));

        // Then
        assertThat(result).isEmpty();
        verify(zSetOperations).remove(indexKey(userId), sessionId.toString());
        verify(zSetOperations, never()).add(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyDouble());
    }

    @Test
    @DisplayName("사용자와 세션 식별자에 대응하는 세션 키가 있으면 유효하다")
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
    @DisplayName("사용자와 세션 식별자에 대응하는 세션 키가 없으면 유효하지 않다")
    void isValid_missingSessionKey_returnsFalse() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(redisTemplate.hasKey(sessionKey(userId, sessionId))).thenReturn(false);

        // When
        boolean result = loginSessionStore.isValid(userId, sessionId);

        // Then
        assertThat(result).isFalse();
        verify(redisTemplate).hasKey(sessionKey(userId, sessionId));
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
    @DisplayName("사용자의 모든 세션 키와 세션 인덱스 키를 삭제한다")
    void deleteByUserId_existingSessions_deletesAllSessionKeysAndIndex() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID firstSessionId = UUID.randomUUID();
        UUID secondSessionId = UUID.randomUUID();
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.range(indexKey(userId), 0, -1))
                .thenReturn(Set.of(firstSessionId.toString(), secondSessionId.toString()));

        // When
        loginSessionStore.deleteByUserId(userId);

        // Then
        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate).delete(keysCaptor.capture());
        assertThat(keysCaptor.getValue()).containsExactlyInAnyOrder(
                sessionKey(userId, firstSessionId),
                sessionKey(userId, secondSessionId));
        verify(redisTemplate).delete(indexKey(userId));
    }

    @Test
    @DisplayName("세션 목록이 비어 있어도 개별 키 삭제 없이 인덱스 키를 삭제한다")
    void deleteByUserId_emptySessions_deletesOnlyIndex() {
        // Given
        UUID userId = UUID.randomUUID();
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.range(indexKey(userId), 0, -1)).thenReturn(Set.of());

        // When
        loginSessionStore.deleteByUserId(userId);

        // Then
        verify(zSetOperations).range(indexKey(userId), 0, -1);
        verify(redisTemplate, never()).delete(org.mockito.ArgumentMatchers.<String>anyCollection());
        verify(redisTemplate).delete(indexKey(userId));
    }

    @Test
    @DisplayName("만료 세션 삭제는 Redis TTL에 맡기고 아무 Redis 작업도 하지 않는다")
    void deleteExpiredSessions_called_doesNothing() {
        // When & Then
        assertThatCode(() -> loginSessionStore.deleteExpiredSessions())
                .doesNotThrowAnyException();
        verifyNoInteractions(redisTemplate, valueOperations, zSetOperations);
    }

    private static String sessionKey(UUID userId, UUID sessionId) {
        return SESSION_KEY_PREFIX + userId + ":" + sessionId;
    }

    private static String indexKey(UUID userId) {
        return INDEX_KEY_PREFIX + userId;
    }
}
