package com.codeit.team5.mopl.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoginSessionTest {

    @Test
    @DisplayName("로그인 세션을 생성한다")
    void create_success() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant expiresAt = Instant.parse("2026-07-08T12:00:00Z");

        // When
        LoginSession loginSession = LoginSession.create(userId, sessionId, expiresAt);

        // Then
        assertThat(loginSession.getUserId()).isEqualTo(userId);
        assertThat(loginSession.getSessionId()).isEqualTo(sessionId);
        assertThat(loginSession.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    @DisplayName("만료 시간이 현재 시각 이후이면 만료되지 않은 세션이다")
    void isExpired_futureExpiresAt_returnsFalse() {
        // Given
        Instant now = Instant.parse("2026-07-08T12:00:00Z");
        LoginSession loginSession = LoginSession.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                now.plusSeconds(1)
        );

        // When
        boolean result = loginSession.isExpired(now);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("만료 시간이 현재 시각과 같거나 이전이면 만료된 세션이다")
    void isExpired_notAfterNow_returnsTrue() {
        // Given
        Instant now = Instant.parse("2026-07-08T12:00:00Z");
        LoginSession loginSession = LoginSession.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                now
        );

        // When
        boolean result = loginSession.isExpired(now);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("세션 식별자가 일치하는지 확인한다")
    void hasSessionId_success() {
        // Given
        UUID sessionId = UUID.randomUUID();
        LoginSession loginSession = LoginSession.create(
                UUID.randomUUID(),
                sessionId,
                Instant.parse("2026-07-08T12:00:00Z")
        );

        // When & Then
        assertThat(loginSession.hasSessionId(sessionId)).isTrue();
        assertThat(loginSession.hasSessionId(UUID.randomUUID())).isFalse();
    }
}
