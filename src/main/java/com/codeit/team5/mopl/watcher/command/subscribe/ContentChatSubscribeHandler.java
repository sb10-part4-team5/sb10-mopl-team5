package com.codeit.team5.mopl.watcher.command.subscribe;

import com.codeit.team5.mopl.watcher.service.WatchingSessionQueryService;
import java.util.UUID;
import org.springframework.stereotype.Component;
import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;
import com.codeit.team5.mopl.global.web.ws.stomp.handler.AbstractStompSubscribeHandler;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import com.codeit.team5.mopl.watcher.service.WatchingSessionCommandService;

@Component
public class ContentChatSubscribeHandler extends AbstractStompSubscribeHandler {

    private final WatchingSessionQueryService service;

    public ContentChatSubscribeHandler(WebSocketSessionStore sessionStore,
        WatchingSessionQueryService service) {
        super(sessionStore, StompConstants.SUB_WATCHING_CONTENT_CHAT);
        this.service = service;
    }

    @Override
    protected void doHandle(UUID contentId, UUID userId) {
        service.ensureWatchingContent(userId, contentId);
    }
}
