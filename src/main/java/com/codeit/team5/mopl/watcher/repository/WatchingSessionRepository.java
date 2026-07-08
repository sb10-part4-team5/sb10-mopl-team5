package com.codeit.team5.mopl.watcher.repository;

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
import com.codeit.team5.mopl.watcher.entity.WatchingSession;

public interface WatchingSessionRepository extends JpaRepository<WatchingSession, UUID> {

    @EntityGraph(attributePaths = {"watcher", "watcher.profileImage", "content", "content.stats",
            "content.thumbnail", "content.contentTags"})
    Optional<WatchingSession> findByWatcherId(UUID watcherId);

    @Modifying
    @Query("DELETE FROM WatchingSession w WHERE w.watcher.id = :watcherId")
    void deleteByWatcherIdDirectly(UUID watcherId);

    @EntityGraph(attributePaths = {"watcher", "watcher.profileImage", "content", "content.stats",
            "content.thumbnail"})
    Window<WatchingSession> findByContentId(UUID contentId, ScrollPosition position, Limit limit,
            Sort sort);

    Long countByContentId(UUID contentId);

    boolean existsByContentIdAndWatcherId(UUID contentId, UUID watcherId);

    boolean existsByWatcherId(UUID watcherId);
}
