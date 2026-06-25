package com.codeit.team5.mopl.auth.service;

import java.time.Instant;
import java.util.UUID;

public interface RefreshTokenStore {

    void save(UUID userId, String rawToken, Instant expiresAt);

    boolean existsValidToken(UUID userId, String rawToken);

    void deleteByUserId(UUID userId);

    void deleteExpiredTokens();
}
