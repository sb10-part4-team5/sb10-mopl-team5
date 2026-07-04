package com.codeit.team5.mopl.playlist.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import com.codeit.team5.mopl.global.exception.BusinessException;

/**
 * PlaylistItemAlreadyExistsException
 */
public class PlaylistItemAlreadyExistsException extends BusinessException {

    public PlaylistItemAlreadyExistsException(UUID playlistId, UUID contentId) {
        super(HttpStatus.BAD_REQUEST, "이미 플레이리스트에 추가된 컨텐츠입니다.",
                Map.of("playlistId", playlistId, "contentId", contentId));
    }
}
