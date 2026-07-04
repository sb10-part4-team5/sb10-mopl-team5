package com.codeit.team5.mopl.playlist.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import com.codeit.team5.mopl.global.exception.BusinessException;

/**
 * PlaylistContentNotFoundException
 */
public class PlaylistContentNotFoundException extends BusinessException {

    public PlaylistContentNotFoundException(UUID contentId) {
        super(HttpStatus.BAD_REQUEST, "컨텐츠를 찾을 수 없습니다.", Map.of("contentId", contentId));
    }

}
