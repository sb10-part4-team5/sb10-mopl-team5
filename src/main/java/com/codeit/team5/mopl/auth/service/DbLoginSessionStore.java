package com.codeit.team5.mopl.auth.service;

import com.codeit.team5.mopl.auth.entity.LoginSession;
import com.codeit.team5.mopl.auth.repository.LoginSessionRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class DbLoginSessionStore implements LoginSessionStore {

    private final LoginSessionRepository loginSessionRepository;

    @Override
    public UUID save(UUID userId, Instant expiresAt) {
        UUID sessionId = UUID.randomUUID();

        LoginSession loginSession = LoginSession.create(
                userId,
                sessionId,
                expiresAt
        );

        loginSessionRepository.save(loginSession);

        return sessionId;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isValid(UUID userId, UUID sessionId) {
        return loginSessionRepository.existsByUserIdAndSessionIdAndExpiresAtAfter(
                userId,
                sessionId,
                Instant.now()
        );
    }

    @Override
    public void deleteByUserId(UUID userId) {
        loginSessionRepository.deleteByUserId(userId);
    }

    @Override
    public void deleteExpiredSessions() {
        loginSessionRepository.deleteByExpiresAtBefore(Instant.now());
    }
}
