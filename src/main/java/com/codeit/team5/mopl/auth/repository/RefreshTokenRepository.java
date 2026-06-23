package com.codeit.team5.mopl.auth.repository;

import com.codeit.team5.mopl.auth.entity.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByUser_Id(UUID userId);

    void deleteByUser_Id(UUID userId);

    void deleteByExpiresAtBefore(Instant now);

    Optional<RefreshToken> findByUser_IdAndTokenHashAndExpiresAtAfter(
            UUID userId,
            String tokenHash,
            Instant now
    );
}
