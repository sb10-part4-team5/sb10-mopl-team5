package com.codeit.team5.mopl.playlist.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import com.codeit.team5.mopl.playlist.entity.PlaylistItem;

public interface PlaylistItemRepository extends JpaRepository<PlaylistItem, UUID> {

    boolean existsByPlaylistIdAndContentId(UUID playlistId, UUID contentId);

    @Modifying
    @Query("delete from PlaylistItem p where p.playlistId = :playlistId and p.content.id = :contentId")
    void deleteByPlaylistIdAndContentIdDirectly(UUID playlistId, UUID contentId);
}
