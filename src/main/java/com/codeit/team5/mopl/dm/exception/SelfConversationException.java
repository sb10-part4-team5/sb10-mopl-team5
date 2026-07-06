package com.codeit.team5.mopl.dm.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class SelfConversationException extends DmException {

    public SelfConversationException(UUID userId) {
        super(HttpStatus.BAD_REQUEST, "자기 자신과 대화를 시작할 수 없습니다.",
                userId == null ? null : Map.of("userId", userId));
    }
}
