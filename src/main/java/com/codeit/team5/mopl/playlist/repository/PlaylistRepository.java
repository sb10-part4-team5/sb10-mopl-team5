package com.codeit.team5.mopl.playlist.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import com.codeit.team5.mopl.playlist.entity.Playlist;
import com.codeit.team5.mopl.playlist.repository.query.PlaylistQueryRepository;

public interface PlaylistRepository extends JpaRepository<Playlist, UUID>, PlaylistQueryRepository {

    boolean existsByIdAndOwnerEmail(UUID id, String email);

    @Modifying
    @Query("DELETE FROM Playlist p WHERE p.id = :id")
    void deleteByIdDirectly(UUID id);
}
