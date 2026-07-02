package com.codeit.team5.mopl.dm.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class NotConversationParticipantException extends DmException {

    public NotConversationParticipantException(UUID userId) {
        super(HttpStatus.FORBIDDEN, "대화 참여자가 아닙니다.",
                userId == null ? null : Map.of("userId", userId));
    }
}
