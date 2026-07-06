package com.codeit.team5.mopl.auth.repository;

import com.codeit.team5.mopl.auth.entity.TemporaryPassword;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TemporaryPasswordRepository extends JpaRepository<TemporaryPassword, UUID> {

    Optional<TemporaryPassword> findByUserId(UUID userId);

    // 동시성 제어를 위해 사용
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select tp from TemporaryPassword tp where tp.user.id = :userId")
    Optional<TemporaryPassword> findByUserIdForUpdate(@Param("userId") UUID userId);

    void deleteByUserId(UUID userId);
}
