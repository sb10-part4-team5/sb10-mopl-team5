package com.codeit.team5.mopl.playlist.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class PlaylistUserNotFoundException extends BusinessException {

    public PlaylistUserNotFoundException(String email) {
        super(HttpStatus.BAD_REQUEST, "유저를 찾을 수 없습니다.",
                Map.of("email", email));
    }
}
