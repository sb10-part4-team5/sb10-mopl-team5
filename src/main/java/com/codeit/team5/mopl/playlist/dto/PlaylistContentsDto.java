package com.codeit.team5.mopl.playlist.dto;

import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.playlist.entity.Playlist;
import java.util.List;

public record PlaylistContentsDto(Playlist playlist, List<Content> contents) {
}
