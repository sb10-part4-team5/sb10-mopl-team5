package com.codeit.team5.mopl.watcher.command.unsubscribe;

import java.util.UUID;
import org.springframework.stereotype.Component;
import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;
import com.codeit.team5.mopl.global.web.ws.stomp.handler.AbstractStompUnsubscribeHandler;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import com.codeit.team5.mopl.watcher.constant.WatcherStatus;
import com.codeit.team5.mopl.watcher.dto.payload.WatchingSessionPayload;
import com.codeit.team5.mopl.watcher.dto.response.WatchingSessionResponse;
import com.codeit.team5.mopl.watcher.provider.WatchingSessionPayloadSender;
import com.codeit.team5.mopl.watcher.service.WatchingSessionCommandService;

@Component
public class WatchingContentUnsubscribeHandler extends AbstractStompUnsubscribeHandler {

    private final WatchingSessionCommandService service;
    private final WatchingSessionPayloadSender payloadSender;

    public WatchingContentUnsubscribeHandler(WebSocketSessionStore sessionStore,
            WatchingSessionCommandService service, WatchingSessionPayloadSender payloadSender) {
        super(sessionStore, StompConstants.SUB_WATCHING_CONTENT);
        this.service = service;
        this.payloadSender = payloadSender;
    }

    @Override
    protected void doHandle(UUID contentId, UUID userId) {
        payloadSender.send(contentId, service.left(userId));
    }
}
