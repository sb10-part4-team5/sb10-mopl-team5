package com.codeit.team5.mopl.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.team5.mopl.auth.entity.LoginSession;
import com.codeit.team5.mopl.global.support.base.BaseRepositoryTest;
import com.codeit.team5.mopl.user.entity.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LoginSessionRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private LoginSessionRepository loginSessionRepository;

    @Test
    @DisplayName("사용자 식별자로 만료되지 않은 로그인 세션을 조회한다")
    void findByUserIdAndExpiresAtAfter_existingSession_returnsSession() {
        // Given
        UUID userId = persistUser("session-current@example.com");
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-08T12:00:00Z");
        LoginSession loginSession = LoginSession.create(userId, sessionId, now.plusSeconds(60));
        persistAndFlush(loginSession);

        // When
        Optional<LoginSession> result = loginSessionRepository.findFirstByUserIdAndExpiresAtAfterOrderByExpiresAtDesc(userId, now);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getSessionId()).isEqualTo(sessionId);
    }

    @Test
    @DisplayName("만료된 로그인 세션은 현재 세션으로 조회되지 않는다")
    void findByUserIdAndExpiresAtAfter_expiredSession_returnsEmpty() {
        // Given
        UUID userId = persistUser("session-expired@example.com");
        Instant now = Instant.parse("2026-07-08T12:00:00Z");
        LoginSession loginSession = LoginSession.create(userId, UUID.randomUUID(), now.minusSeconds(1));
        persistAndFlush(loginSession);

        // When
        Optional<LoginSession> result = loginSessionRepository.findFirstByUserIdAndExpiresAtAfterOrderByExpiresAtDesc(userId, now);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("사용자와 세션 식별자 기준으로 만료되지 않은 세션 존재 여부를 확인한다")
    void existsByUserIdAndSessionIdAndExpiresAtAfter_existingSession_returnsTrue() {
        // Given
        UUID userId = persistUser("session-exists@example.com");
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-08T12:00:00Z");
        LoginSession loginSession = LoginSession.create(userId, sessionId, now.plusSeconds(60));
        persistAndFlush(loginSession);

        // When
        boolean result = loginSessionRepository.existsByUserIdAndSessionIdAndExpiresAtAfter(userId, sessionId, now);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("사용자 식별자로 로그인 세션을 삭제한다")
    void deleteByUserId_deletesSessions() {
        // Given
        UUID userId = persistUser("session-delete-target@example.com");
        UUID anotherUserId = persistUser("session-delete-other@example.com");
        persistAndFlush(LoginSession.create(userId, UUID.randomUUID(), Instant.now().plusSeconds(60)));
        persistAndFlush(LoginSession.create(anotherUserId, UUID.randomUUID(), Instant.now().plusSeconds(60)));

        // When
        loginSessionRepository.deleteByUserId(userId);
        flush();

        // Then
        assertThat(loginSessionRepository.findAll())
                .extracting(LoginSession::getUserId)
                .doesNotContain(userId);
    }

    @Test
    @DisplayName("만료된 로그인 세션을 삭제한다")
    void deleteByExpiresAtBefore_deletesExpiredSessions() {
        // Given
        Instant now = Instant.parse("2026-07-08T12:00:00Z");
        UUID expiredUserId = persistUser("session-delete-expired@example.com");
        UUID activeUserId = persistUser("session-delete-active@example.com");
        persistAndFlush(LoginSession.create(expiredUserId, UUID.randomUUID(), now.minusSeconds(1)));
        persistAndFlush(LoginSession.create(activeUserId, UUID.randomUUID(), now.plusSeconds(60)));

        // When
        loginSessionRepository.deleteByExpiresAtBefore(now);
        flush();

        // Then
        assertThat(loginSessionRepository.findAll())
                .extracting(LoginSession::getUserId)
                .containsExactly(activeUserId);
    }

    private UUID persistUser(String email) {
        User user = persistAndFlush(User.create(email, "encoded-password", "사용자"));
        return user.getId();
    }
}
