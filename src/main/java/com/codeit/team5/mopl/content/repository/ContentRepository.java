package com.codeit.team5.mopl.content.repository;

import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.repository.querydsl.ContentRepositoryCustom;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentRepository extends JpaRepository<Content, UUID>, ContentRepositoryCustom {

    @EntityGraph(attributePaths = {"thumbnail", "stats", "contentTags", "contentTags.tag"})
    Optional<Content> findWithStatsAndTagsById(UUID id);
}
