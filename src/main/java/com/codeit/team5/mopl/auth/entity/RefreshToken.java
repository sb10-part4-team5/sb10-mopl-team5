package com.codeit.team5.mopl.auth.entity;

import com.codeit.team5.mopl.global.entity.BaseEntity;
import com.codeit.team5.mopl.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public static RefreshToken create(
            User user,
            String tokenHash,
            Instant expiresAt
    ) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.user = Objects.requireNonNull(user, "user must not be null");
        refreshToken.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash must not be null");
        refreshToken.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        return refreshToken;
    }

    public boolean isExpired() {
        return !expiresAt.isAfter(Instant.now());
    }
}
