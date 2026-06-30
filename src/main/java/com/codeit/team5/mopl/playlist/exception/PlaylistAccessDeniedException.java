package com.codeit.team5.mopl.playlist.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class PlaylistAccessDeniedException extends BusinessException {

    public PlaylistAccessDeniedException(UUID id, String email) {
        super(HttpStatus.FORBIDDEN, "플레이리스트 접근 권한이 없습니다.",
                Map.of("playlistId", id, "email", email));
    }
}
