package com.codeit.team5.mopl.auth.repository;

import com.codeit.team5.mopl.auth.entity.TemporaryPassword;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemporaryPasswordRepository extends JpaRepository<TemporaryPassword, UUID> {

    Optional<TemporaryPassword> findTopByUserIdOrderByCreatedAtDesc(UUID userId);

    void deleteByUserId(UUID userId);
}
