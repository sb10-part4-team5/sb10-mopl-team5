package com.codeit.team5.mopl.notification.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class NotificationUserNotFoundException extends BusinessException {

    public NotificationUserNotFoundException(UUID userId) {
        super(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다.", Map.of("userId", userId));
    }
}
