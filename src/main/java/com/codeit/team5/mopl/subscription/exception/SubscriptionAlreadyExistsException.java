package com.codeit.team5.mopl.subscription.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import com.codeit.team5.mopl.global.exception.BusinessException;

/**
 * SubscriptionAlreadyExistsException
 */
public class SubscriptionAlreadyExistsException extends BusinessException {

    public SubscriptionAlreadyExistsException(UUID userId, UUID playlistId) {
        super(HttpStatus.BAD_REQUEST, "이미 플레이리스트를 구독하고 있습니다.",
                Map.of("userId", userId, "playlistId", playlistId));
    }

}
