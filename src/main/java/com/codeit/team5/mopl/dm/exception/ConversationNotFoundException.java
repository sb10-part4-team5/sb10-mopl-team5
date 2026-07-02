package com.codeit.team5.mopl.dm.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class ConversationNotFoundException extends DmException {

    public ConversationNotFoundException(UUID conversationId) {
        super(HttpStatus.NOT_FOUND, "대화를 찾을 수 없습니다.",
                conversationId == null ? null : Map.of("conversationId", conversationId));
    }

    private ConversationNotFoundException(Map<String, Object> details) {
        super(HttpStatus.NOT_FOUND, "대화를 찾을 수 없습니다.", details);
    }

    public static ConversationNotFoundException withUser(UUID withUserId) {
        return new ConversationNotFoundException(
                withUserId == null ? null : Map.of("withUserId", withUserId));
    }
}
