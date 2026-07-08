package com.codeit.team5.mopl.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.auth.entity.LoginSession;
import com.codeit.team5.mopl.auth.repository.LoginSessionRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbLoginSessionStoreTest {

    @Mock
    private LoginSessionRepository loginSessionRepository;

    @InjectMocks
    private DbLoginSessionStore loginSessionStore;

    @Test
    @DisplayName("로그인 세션을 저장하고 생성된 세션 식별자를 반환한다")
    void save_createsLoginSessionAndReturnsSessionId() {
        // Given
        UUID userId = UUID.randomUUID();
        Instant expiresAt = Instant.parse("2026-07-08T12:00:00Z");

        // When
        UUID result = loginSessionStore.save(userId, expiresAt);

        // Then
        ArgumentCaptor<LoginSession> captor = ArgumentCaptor.forClass(LoginSession.class);
        verify(loginSessionRepository).save(captor.capture());

        LoginSession savedSession = captor.getValue();
        assertThat(result).isEqualTo(savedSession.getSessionId());
        assertThat(savedSession.getUserId()).isEqualTo(userId);
        assertThat(savedSession.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    @DisplayName("현재 유효한 로그인 세션 식별자를 조회한다")
    void findCurrentSessionId_existingSession_returnsSessionId() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        LoginSession loginSession = LoginSession.create(
                userId,
                sessionId,
                Instant.now().plusSeconds(60)
        );
        when(loginSessionRepository.findFirstByUserIdAndExpiresAtAfter(eq(userId), any(Instant.class)))
                .thenReturn(Optional.of(loginSession));

        // When
        Optional<UUID> result = loginSessionStore.findCurrentSessionId(userId);

        // Then
        assertThat(result).contains(sessionId);
        verify(loginSessionRepository).findFirstByUserIdAndExpiresAtAfter(eq(userId), any(Instant.class));
    }

    @Test
    @DisplayName("현재 유효한 로그인 세션이 없으면 빈 Optional을 반환한다")
    void findCurrentSessionId_missingSession_returnsEmpty() {
        // Given
        UUID userId = UUID.randomUUID();
        when(loginSessionRepository.findFirstByUserIdAndExpiresAtAfter(eq(userId), any(Instant.class)))
                .thenReturn(Optional.empty());

        // When
        Optional<UUID> result = loginSessionStore.findCurrentSessionId(userId);

        // Then
        assertThat(result).isEmpty();
        verify(loginSessionRepository).findFirstByUserIdAndExpiresAtAfter(eq(userId), any(Instant.class));
    }

    @Test
    @DisplayName("사용자와 세션 식별자 기준으로 유효한 세션 존재 여부를 확인한다")
    void isValid_delegatesToRepository() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(loginSessionRepository.existsByUserIdAndSessionIdAndExpiresAtAfter(
                eq(userId),
                eq(sessionId),
                any(Instant.class)
        )).thenReturn(true);

        // When
        boolean result = loginSessionStore.isValid(userId, sessionId);

        // Then
        assertThat(result).isTrue();
        verify(loginSessionRepository).existsByUserIdAndSessionIdAndExpiresAtAfter(
                eq(userId),
                eq(sessionId),
                any(Instant.class)
        );
    }

    @Test
    @DisplayName("사용자 식별자로 로그인 세션을 삭제한다")
    void deleteByUserId_delegatesToRepository() {
        // Given
        UUID userId = UUID.randomUUID();

        // When
        loginSessionStore.deleteByUserId(userId);

        // Then
        verify(loginSessionRepository).deleteByUserId(userId);
    }

    @Test
    @DisplayName("만료된 로그인 세션을 삭제한다")
    void deleteExpiredSessions_delegatesToRepository() {
        // When
        loginSessionStore.deleteExpiredSessions();

        // Then
        verify(loginSessionRepository).deleteByExpiresAtBefore(any(Instant.class));
    }
}
