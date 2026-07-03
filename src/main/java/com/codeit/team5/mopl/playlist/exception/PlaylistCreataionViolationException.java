package com.codeit.team5.mopl.playlist.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;
import com.codeit.team5.mopl.global.exception.BusinessException;

public class PlaylistCreataionViolationException extends BusinessException {

    public PlaylistCreataionViolationException(String message, Map<String, Object> details) {
        super(HttpStatus.BAD_REQUEST, message, details);
    }
}
