package com.codeit.team5.mopl.follow.repository;

import com.codeit.team5.mopl.follow.entity.Follow;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FollowRepository extends JpaRepository<Follow, UUID> {

    boolean existsByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);

    Optional<Follow> findByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);

    long countByFolloweeId(UUID followeeId);

    @Query("SELECT f.follower.id FROM Follow f WHERE f.followee.id = :followeeId")
    List<UUID> findFollowerIdsByFolloweeId(UUID followeeId);
}
