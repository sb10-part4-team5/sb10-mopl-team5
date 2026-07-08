package com.codeit.team5.mopl.auth.repository;

import com.codeit.team5.mopl.auth.entity.LoginSession;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginSessionRepository extends JpaRepository<LoginSession, UUID> {

    Optional<LoginSession> findFirstByUserIdAndExpiresAtAfterOrderByExpiresAtDesc(UUID userId, Instant now);

    boolean existsByUserIdAndSessionIdAndExpiresAtAfter(
            UUID userId,
            UUID sessionId,
            Instant now
    );

    void deleteByUserId(UUID userId);

    void deleteByExpiresAtBefore(Instant now);
}
