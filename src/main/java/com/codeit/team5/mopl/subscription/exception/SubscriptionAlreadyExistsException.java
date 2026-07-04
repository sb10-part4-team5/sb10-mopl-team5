package com.codeit.team5.mopl.subscription.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import com.codeit.team5.mopl.global.exception.BusinessException;

/**
 * SubscriptionAlreadyExistsException
 */
public class SubscriptionAlreadyExistsException extends BusinessException {

    public SubscriptionAlreadyExistsException(String email, UUID playlistId) {
        super(HttpStatus.BAD_REQUEST, "이미 플레이리스트를 구독하고 있습니다.",
                Map.of("email", email, "playlistId", playlistId));
    }

}
