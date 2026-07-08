package com.codeit.team5.mopl.watcher.command.unsubscribe;

import java.util.UUID;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;
import com.codeit.team5.mopl.global.web.ws.stomp.handler.AbstractStompUnsubscribeHandler;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;

@Component
public class ContentChatUnsubscribeHandler extends AbstractStompUnsubscribeHandler {

    public ContentChatUnsubscribeHandler(WebSocketSessionStore sessionStore) {
        super(sessionStore, StompConstants.SUB_WATCHING_CONTENT_CHAT);
    }

    @Override
    protected void doHandle(UUID targetId, UUID userId, StompHeaderAccessor accessor) {
        // nothing to handle
    }
}
