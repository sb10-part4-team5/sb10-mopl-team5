package com.codeit.team5.mopl.content.repository;

import com.codeit.team5.mopl.content.entity.ContentStats;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentStatsRepository extends JpaRepository<ContentStats, UUID> {

    @Modifying
    @Query("""
        UPDATE ContentStats s
        SET s.reviewCount = s.reviewCount + :countDelta,
            s.ratingSum   = CASE WHEN s.reviewCount + :countDelta = 0
                                 THEN 0.0
                                 ELSE s.ratingSum + :ratingDelta
                            END
        WHERE s.id = :contentId
        """)
    void applyStatDelta(
        @Param("contentId") UUID contentId,
        @Param("ratingDelta") double ratingDelta,
        @Param("countDelta") int countDelta
    );

    @Modifying
    @Query("update ContentStats s set s.watcherCount = s.watcherCount - 1 where s.id = :id and s.watcherCount > 0")
    void decreaseWatcherCountById(UUID id);

    @Modifying
    @Query("update ContentStats s set s.watcherCount = s.watcherCount + 1 where s.id = :id")
    void increaseWatcherCountById(UUID id);
}
