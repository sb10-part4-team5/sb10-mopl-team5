package com.codeit.team5.mopl.watcher.command.subscribe;

import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;
import com.codeit.team5.mopl.global.web.ws.stomp.handler.AbstractStompSubscribeHandler;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import com.codeit.team5.mopl.watcher.constant.WatcherStatus;
import com.codeit.team5.mopl.watcher.dto.payload.WatchingSessionPayload;
import com.codeit.team5.mopl.watcher.dto.response.WatchingSessionResponse;
import com.codeit.team5.mopl.watcher.provider.WatchingSessionPayloadSender;
import com.codeit.team5.mopl.watcher.service.WatchingSessionService;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class WatchingContentSubscribeHandler extends AbstractStompSubscribeHandler {

    private final WatchingSessionService service;
    private final WatchingSessionPayloadSender payloadSender;

    public WatchingContentSubscribeHandler(WebSocketSessionStore sessionStore,
            WatchingSessionService service, WatchingSessionPayloadSender payloadSender) {
        super(sessionStore, StompConstants.SUB_WATCHING_CONTENT_PATTERN);
        this.service = service;
        this.payloadSender = payloadSender;
    }

    @Override
    protected void doHandle(UUID contentId, String email) {
        WatchingSessionResponse response = service.create(contentId, email);
        long watchCount = service.getCurrentWatchingContentView(contentId);
        payloadSender.send(contentId,
                new WatchingSessionPayload(WatcherStatus.JOIN, response, watchCount));
    }
}
