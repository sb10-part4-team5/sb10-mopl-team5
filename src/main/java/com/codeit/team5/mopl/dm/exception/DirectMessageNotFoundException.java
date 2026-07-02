package com.codeit.team5.mopl.dm.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class DirectMessageNotFoundException extends DmException {

    public DirectMessageNotFoundException(UUID directMessageId) {
        super(HttpStatus.NOT_FOUND, "메시지를 찾을 수 없습니다.",
                directMessageId == null ? null : Map.of("directMessageId", directMessageId));
    }
}
