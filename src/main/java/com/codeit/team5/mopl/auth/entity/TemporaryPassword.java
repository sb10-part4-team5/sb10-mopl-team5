package com.codeit.team5.mopl.auth.entity;

import com.codeit.team5.mopl.global.entity.BaseEntity;
import com.codeit.team5.mopl.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Duration;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "temporary_passwords",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_temporary_passwords_user_id",
                        columnNames = "user_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TemporaryPassword extends BaseEntity {

    private static final Duration EXPIRATION_DURATION = Duration.ofMinutes(3);

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public static TemporaryPassword create(
            User user, String passwordHash, Instant issuedAt
    ) {
        TemporaryPassword password = new TemporaryPassword();
        password.user = user;
        password.passwordHash = passwordHash;
        password.expiresAt = issuedAt.plus(EXPIRATION_DURATION);

        return password;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isValidAt(Instant now) {
        return !isExpired(now);
    }

    public void reissue(String passwordHash, Instant issuedAt) {
        this.passwordHash = passwordHash;
        this.expiresAt = issuedAt.plus(EXPIRATION_DURATION);
    }
}
