package com.codeit.team5.mopl.playlist.repository.query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.codeit.team5.mopl.playlist.dto.PlaylistContentsDto;
import com.codeit.team5.mopl.playlist.dto.PlaylistCursorCommand;

public interface PlaylistQueryRepository {

    Optional<PlaylistContentsDto> findByIdWithContents(UUID id, UUID userId);

    List<PlaylistContentsDto> findByCursor(PlaylistCursorCommand request, UUID userId);

    long countByCommand(PlaylistCursorCommand request);
}
