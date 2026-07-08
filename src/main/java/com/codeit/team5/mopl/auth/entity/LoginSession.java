package com.codeit.team5.mopl.auth.entity;

import com.codeit.team5.mopl.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "login_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginSession extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "session_id", nullable = false, unique = true)
    private UUID sessionId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    private LoginSession(UUID userId, UUID sessionId, Instant expiresAt) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.expiresAt = expiresAt;
    }

    public static LoginSession create(UUID userId, UUID sessionId, Instant expiresAt) {
        return new LoginSession(userId, sessionId, expiresAt);
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public boolean hasSessionId(UUID sessionId) {
        return this.sessionId.equals(sessionId);
    }

    public void extendExpiresAt(Instant expiresAt) {
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }
}
