package com.codeit.team5.mopl.auth.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface LoginSessionStore {

    UUID save(UUID userId, Instant expiresAt);

    Optional<UUID> findCurrentSessionId(UUID userId);

    boolean isValid(UUID userId, UUID sessionId);

    void deleteByUserId(UUID userId);

    void deleteExpiredSessions();
}
