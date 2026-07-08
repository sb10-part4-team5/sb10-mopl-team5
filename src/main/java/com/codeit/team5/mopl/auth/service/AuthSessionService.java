package com.codeit.team5.mopl.auth.service;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthSessionService {

    private final LoginSessionStore loginSessionStore;
    private final RefreshTokenStore refreshTokenStore;

    public UUID replaceUserSession(UUID userId, Instant expiresAt) {
        invalidateUserSessions(userId);
        return loginSessionStore.save(userId, expiresAt);
    }

    @Transactional(readOnly = true)
    public boolean isValidSession(UUID userId, UUID sessionId) {
        return loginSessionStore.isValid(userId, sessionId);
    }

    public void invalidateUserSessions(UUID userId) {
        loginSessionStore.deleteByUserId(userId);
        refreshTokenStore.deleteByUserId(userId);
    }

    public void deleteExpiredSessions() {
        loginSessionStore.deleteExpiredSessions();
    }
}
