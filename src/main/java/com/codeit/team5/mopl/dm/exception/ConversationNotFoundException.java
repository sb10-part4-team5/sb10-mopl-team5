package com.codeit.team5.mopl.dm.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class ConversationNotFoundException extends DmException {

    public ConversationNotFoundException(UUID conversationId) {
        super(HttpStatus.NOT_FOUND, "대화를 찾을 수 없습니다.",
                conversationId == null ? null : Map.of("conversationId", conversationId));
    }
}
