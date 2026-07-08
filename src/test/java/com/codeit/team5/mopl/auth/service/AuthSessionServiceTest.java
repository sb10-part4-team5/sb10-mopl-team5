package com.codeit.team5.mopl.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.auth.exception.SessionInvalidException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthSessionServiceTest {

    @Mock
    private LoginSessionStore loginSessionStore;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @InjectMocks
    private AuthSessionService authSessionService;

    @Test
    @DisplayName("현재 로그인 세션 식별자를 조회한다")
    void getCurrentSessionId_success() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(loginSessionStore.findCurrentSessionId(userId)).thenReturn(Optional.of(sessionId));

        // When
        UUID result = authSessionService.getCurrentSessionId(userId);

        // Then
        assertThat(result).isEqualTo(sessionId);
        verify(loginSessionStore).findCurrentSessionId(userId);
    }

    @Test
    @DisplayName("현재 로그인 세션이 없으면 예외를 던진다")
    void getCurrentSessionId_missingSession_throwsException() {
        // Given
        UUID userId = UUID.randomUUID();
        when(loginSessionStore.findCurrentSessionId(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authSessionService.getCurrentSessionId(userId))
                .isInstanceOf(SessionInvalidException.class)
                .hasMessage("Invalid login session");
        verify(loginSessionStore).findCurrentSessionId(userId);
    }

    @Test
    @DisplayName("사용자 세션 교체 시 기존 로그인 세션과 리프레시 토큰을 삭제한 뒤 새 세션을 저장한다")
    void replaceUserSession_invalidatesAndSavesNewSession() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant expiresAt = Instant.parse("2026-07-08T12:00:00Z");
        when(loginSessionStore.save(userId, expiresAt)).thenReturn(sessionId);

        // When
        UUID result = authSessionService.replaceUserSession(userId, expiresAt);

        // Then
        assertThat(result).isEqualTo(sessionId);
        verify(loginSessionStore).deleteByUserId(userId);
        verify(refreshTokenStore).deleteByUserId(userId);
        verify(loginSessionStore).save(userId, expiresAt);
    }

    @Test
    @DisplayName("세션 유효성 확인을 로그인 세션 저장소에 위임한다")
    void isValidSession_delegatesToStore() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(loginSessionStore.isValid(userId, sessionId)).thenReturn(true);

        // When
        boolean result = authSessionService.isValidSession(userId, sessionId);

        // Then
        assertThat(result).isTrue();
        verify(loginSessionStore).isValid(userId, sessionId);
    }

    @Test
    @DisplayName("사용자 세션 무효화 시 로그인 세션과 리프레시 토큰을 모두 삭제한다")
    void invalidateUserSessions_deletesLoginSessionAndRefreshToken() {
        // Given
        UUID userId = UUID.randomUUID();

        // When
        authSessionService.invalidateUserSessions(userId);

        // Then
        verify(loginSessionStore).deleteByUserId(userId);
        verify(refreshTokenStore).deleteByUserId(userId);
    }

    @Test
    @DisplayName("만료된 로그인 세션 삭제를 저장소에 위임한다")
    void deleteExpiredSessions_delegatesToStore() {
        // When
        authSessionService.deleteExpiredSessions();

        // Then
        verify(loginSessionStore).deleteExpiredSessions();
    }
}
