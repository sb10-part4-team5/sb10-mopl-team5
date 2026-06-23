package com.codeit.team5.mopl.watcher.repository;

import com.codeit.team5.mopl.watcher.entity.WatchingSession;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface WatchingSessionRepository extends JpaRepository<WatchingSession, UUID> {

    @EntityGraph(attributePaths = {"user", "content"})
    Optional<WatchingSession> findByUser_Id(UUID userId);

    @Modifying
    @Query("DELETE FROM WatchingSession w WHERE w.user.id = :userId")
    void deleteByUserIdDirectly(UUID userId);

    @EntityGraph(attributePaths = {"user", "content"})
    Window<WatchingSession> findByContent_Id(UUID contentId, ScrollPosition position, Limit limit,
            Sort sort);

    boolean existsByUser_Id(UUID userId);
}
