package com.codeit.team5.mopl.user.repository;

import com.codeit.team5.mopl.user.entity.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    // refresh Token 재발급/저장을 직렬화하기 위함
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") UUID id);

    // N+1 문제 방지 update용 조회 메서드
    @EntityGraph(attributePaths = {"profileImage"})
    Optional<User> findWithProfileImageById(UUID id);
}
