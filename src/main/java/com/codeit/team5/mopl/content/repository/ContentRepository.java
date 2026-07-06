package com.codeit.team5.mopl.content.repository;

import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentSource;
import com.codeit.team5.mopl.content.repository.querydsl.ContentQueryRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentRepository extends JpaRepository<Content, UUID>, ContentQueryRepository {

    @EntityGraph(attributePaths = {"thumbnail", "stats", "contentTags", "contentTags.tag"})
    Optional<Content> findWithStatsAndTagsById(UUID id);

    @Query("SELECT c.externalId FROM Content c WHERE c.source = :source AND c.externalId IN :externalIds")
    Set<String> findExternalIdsBySourceAndExternalIdIn(
            @Param("source") ContentSource source,
            @Param("externalIds") List<String> externalIds
    );
}
