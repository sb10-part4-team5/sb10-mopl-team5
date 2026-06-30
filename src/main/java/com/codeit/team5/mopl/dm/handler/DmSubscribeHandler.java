package com.codeit.team5.mopl.dm.handler;

import com.codeit.team5.mopl.dm.service.DmService;
import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;
import com.codeit.team5.mopl.global.web.ws.stomp.handler.AbstractStompSubscribeHandler;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DmSubscribeHandler extends AbstractStompSubscribeHandler {

    private final DmService dmService;

    public DmSubscribeHandler(WebSocketSessionStore sessionStore, DmService dmService) {
        super(sessionStore, StompConstants.SUB_CONVERSATION_DM);
        this.dmService = dmService;
    }

    @Override
    protected void doHandle(UUID conversationId, String email) {
        dmService.validateParticipant(conversationId, email);
    }
}
