package com.codeit.team5.mopl.user.repository;

import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.querydsl.UserQueryRepository;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID>, UserQueryRepository {
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    // refresh Token 재발급/저장을 직렬화하기 위함
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") UUID id);

    // 프로필 이미지를 fetch join으로 함께 조회 (update 시 N+1 방지)
    @Query("select u from User u left join fetch u.profileImage where u.id = :id")
    Optional<User> findWithProfileImageById(@Param("id") UUID id);
}
