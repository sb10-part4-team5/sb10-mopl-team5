package com.codeit.team5.mopl.notification.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class NotificationNotFoundException extends BusinessException {

    public NotificationNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다: id=" + id);
    }
}
