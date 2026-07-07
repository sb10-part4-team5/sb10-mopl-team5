package com.codeit.team5.mopl.dm.handler;

import com.codeit.team5.mopl.dm.service.ConversationService;
import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;
import com.codeit.team5.mopl.global.web.ws.stomp.handler.AbstractStompSubscribeHandler;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DmSubscribeHandler extends AbstractStompSubscribeHandler {

    private final ConversationService conversationService;

    public DmSubscribeHandler(WebSocketSessionStore sessionStore, ConversationService conversationService) {
        super(sessionStore, StompConstants.SUB_CONVERSATION_DM);
        this.conversationService = conversationService;
    }

    @Override
    protected void doHandle(UUID targetId, UUID userId) {
        conversationService.validateParticipant(targetId, userId);
    }
}
