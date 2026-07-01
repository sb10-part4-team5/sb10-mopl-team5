package com.codeit.team5.mopl.subscription.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class SubscriptionPlaylistNotFoundException extends BusinessException {

    public SubscriptionPlaylistNotFoundException(UUID playlistId) {
        super(HttpStatus.NOT_FOUND, "플레이리스트를 찾을 수 없습니다.", Map.of("playlistId", playlistId));
    }
}
