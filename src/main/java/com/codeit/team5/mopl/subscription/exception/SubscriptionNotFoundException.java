package com.codeit.team5.mopl.subscription.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class SubscriptionNotFoundException extends BusinessException {

    public SubscriptionNotFoundException(UUID playlistId, String email) {
        super(HttpStatus.NOT_FOUND, "유저의 플레이리스트를 찾을 수 없습니다.",
                Map.of("playlistId", playlistId, "email", email));
    }
}
