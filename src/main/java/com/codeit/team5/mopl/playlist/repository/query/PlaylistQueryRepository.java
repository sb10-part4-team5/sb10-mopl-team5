package com.codeit.team5.mopl.playlist.repository.query;

import com.codeit.team5.mopl.playlist.dto.PlaylistCursorCommand;
import com.codeit.team5.mopl.playlist.entity.Playlist;
import java.util.List;

public interface PlaylistQueryRepository {

    List<Playlist> findByCursor(PlaylistCursorCommand request);

    long countByCommand(PlaylistCursorCommand request);
}
