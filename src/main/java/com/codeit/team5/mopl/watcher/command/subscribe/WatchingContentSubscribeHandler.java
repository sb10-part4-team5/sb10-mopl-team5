package com.codeit.team5.mopl.watcher.command.subscribe;

import java.util.UUID;
import org.springframework.stereotype.Component;
import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;
import com.codeit.team5.mopl.global.web.ws.stomp.handler.AbstractStompSubscribeHandler;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import com.codeit.team5.mopl.watcher.service.WatchingSessionCommandService;

@Component
public class WatchingContentSubscribeHandler extends AbstractStompSubscribeHandler {

    private final WatchingSessionCommandService service;

    public WatchingContentSubscribeHandler(WebSocketSessionStore sessionStore,
            WatchingSessionCommandService service) {
        super(sessionStore, StompConstants.SUB_WATCHING_CONTENT);
        this.service = service;
    }

    @Override
    protected void doHandle(UUID contentId, UUID userId) {
        service.join(contentId, userId);
    }
}
