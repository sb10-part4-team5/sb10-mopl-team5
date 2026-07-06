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
    int applyStatDelta(
        @Param("contentId") UUID contentId,
        @Param("ratingDelta") double ratingDelta,
        @Param("countDelta") int countDelta
    );
}
