package com.codeit.team5.mopl.auth.service;

import java.time.Instant;
import java.util.UUID;

public interface RefreshTokenStore {

    void save(UUID userId, String rawToken, Instant expiresAt);

    boolean existsValidToken(UUID userId, String rawToken);

    boolean rotateIfValid(UUID userId, String oldToken, String newToken, Instant expiresAt);

    void deleteByUserId(UUID userId);

    void deleteExpiredTokens();
}
