package com.codeit.team5.mopl.auth.service;

import com.codeit.team5.mopl.auth.exception.SessionInvalidException;
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

    @Transactional(readOnly = true)
    public UUID getCurrentSessionId(UUID userId) {
        return loginSessionStore.findCurrentSessionId(userId)
                .orElseThrow(() -> new SessionInvalidException("Invalid login session"));
    }

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
