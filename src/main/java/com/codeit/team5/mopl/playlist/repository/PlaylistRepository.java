package com.codeit.team5.mopl.playlist.repository;

import com.codeit.team5.mopl.playlist.entity.Playlist;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PlaylistRepository extends JpaRepository<Playlist, UUID> {

    @EntityGraph(attributePaths = {"owner", "owner.profileImage"})
    Optional<Playlist> findOwnerById(UUID id);

    boolean existsByIdAndOwnerEmail(UUID id, String email);

    @Modifying
    @Query("DELETE FROM Playlist p WHERE p.id = :id")
    void deleteByIdDirectly(UUID id);
}
