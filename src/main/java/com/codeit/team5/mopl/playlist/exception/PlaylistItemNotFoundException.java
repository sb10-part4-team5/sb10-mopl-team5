package com.codeit.team5.mopl.playlist.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import com.codeit.team5.mopl.global.exception.BusinessException;

/**
 * PlaylistItemNotFoundException
 */
public class PlaylistItemNotFoundException extends BusinessException {

    public PlaylistItemNotFoundException(UUID playlistId, UUID contentId) {
        super(HttpStatus.BAD_REQUEST, "플레이리스트에 추가된 컨텐츠를 찾을 수 없습니다.",
                Map.of("playlistId", playlistId, "contentId", contentId));
    }

}
