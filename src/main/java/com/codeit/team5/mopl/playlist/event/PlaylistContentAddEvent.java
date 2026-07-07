package com.codeit.team5.mopl.playlist.event;

import com.codeit.team5.mopl.content.exception.InvalidContentTitleException;
import com.codeit.team5.mopl.playlist.exception.InvalidPlaylistNameException;
import java.util.UUID;
import org.springframework.util.StringUtils;

// 구독중인 플레이리스트에 콘텐츠가 추가되었을 때 발행되는 이벤트
public record PlaylistContentAddEvent(
    UUID playlistId,
    String playlistName,
    String contentTitle
) {
    public PlaylistContentAddEvent {
        if (playlistId == null) {
            throw new IllegalArgumentException("playlistId must not be null");
        }
        if (!StringUtils.hasText(playlistName)) {
            throw new InvalidPlaylistNameException();
        }
        if (!StringUtils.hasText(contentTitle)) {
            throw new InvalidContentTitleException();
        }
    }
}
