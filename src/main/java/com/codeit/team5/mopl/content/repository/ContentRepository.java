package com.codeit.team5.mopl.content.repository;

import com.codeit.team5.mopl.binarycontent.entity.BinaryContentUploadStatus;
import com.codeit.team5.mopl.content.entity.Content;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentRepository extends JpaRepository<Content, UUID> {

    @EntityGraph(attributePaths = {"stats", "contentTags"})
    Optional<Content> findWithStatsAndTagsById(UUID id);

    @Modifying
    @Query("UPDATE Content c SET c.thumbnailUploadStatus = :status WHERE c.id = :id")
    void updateThumbnailUploadStatus(@Param("id") UUID id, @Param("status") BinaryContentUploadStatus status);
}
