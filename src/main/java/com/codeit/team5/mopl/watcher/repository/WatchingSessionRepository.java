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

    @EntityGraph(attributePaths = {"watcher", "content", "content.stats", "content.thumbnail", "content.contentTags"})
    Optional<WatchingSession> findByWatcherId(UUID watcherId);

    @Modifying
    @Query("DELETE FROM WatchingSession w WHERE w.watcher.email = :email")
    void deleteByWatcherEmailDirectly(String email);

    @EntityGraph(attributePaths = {"watcher", "content", "content.stats", "content.thumbnail"})
    Window<WatchingSession> findByContentId(UUID contentId, ScrollPosition position, Limit limit,
            Sort sort);

    Long countByContentId(UUID contentId);

    boolean existsByWatcherEmailAndContentId(String email, UUID contentId);

    @EntityGraph(attributePaths = {"watcher", "content", "content.stats", "content.thumbnail", "content.contentTags"})
    Optional<WatchingSession> findByWatcherEmail(String email);

    boolean existsByWatcherEmail(String email);
}
