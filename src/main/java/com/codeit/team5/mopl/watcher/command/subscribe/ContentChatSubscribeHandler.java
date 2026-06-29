package com.codeit.team5.mopl.watcher.command.subscribe;

import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;
import com.codeit.team5.mopl.global.web.ws.stomp.handler.AbstractStompSubscribeHandler;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import com.codeit.team5.mopl.watcher.exception.WatchingSessionNotFoundException;
import com.codeit.team5.mopl.watcher.service.WatchingSessionService;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ContentChatSubscribeHandler extends AbstractStompSubscribeHandler {

    private final WatchingSessionService service;

    public ContentChatSubscribeHandler(WebSocketSessionStore sessionStore,
            WatchingSessionService service) {
        super(sessionStore, StompConstants.SUB_WATCHING_CONTENT_CHAT);
        this.service = service;
    }

    @Override
    protected void doHandle(UUID contentId, String email) {
        service.ensureWatchingContent(email, contentId);
    }
}
