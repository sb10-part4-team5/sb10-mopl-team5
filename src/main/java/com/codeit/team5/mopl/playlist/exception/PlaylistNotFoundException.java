package com.codeit.team5.mopl.playlist.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class PlaylistNotFoundException extends BusinessException {

    public PlaylistNotFoundException(UUID id) {
        super(HttpStatus.BAD_REQUEST, "플레이리스트를 찾을 수 없습니다.",
                Map.of("playlistId", id));
    }
}
