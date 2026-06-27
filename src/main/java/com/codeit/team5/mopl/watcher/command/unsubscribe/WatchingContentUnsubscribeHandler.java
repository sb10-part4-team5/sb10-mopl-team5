package com.codeit.team5.mopl.watcher.command.unsubscribe;

import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;
import com.codeit.team5.mopl.global.web.ws.stomp.handler.AbstractStompUnsubscribeHandler;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import com.codeit.team5.mopl.watcher.constant.WatcherStatus;
import com.codeit.team5.mopl.watcher.dto.payload.WatchingSessionPayload;
import com.codeit.team5.mopl.watcher.dto.response.WatchingSessionResponse;
import com.codeit.team5.mopl.watcher.provider.WatchingSessionPayloadSender;
import com.codeit.team5.mopl.watcher.service.WatchingSessionService;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class WatchingContentUnsubscribeHandler extends AbstractStompUnsubscribeHandler {

    private final WatchingSessionService service;
    private final WatchingSessionPayloadSender payloadSender;

    public WatchingContentUnsubscribeHandler(WebSocketSessionStore sessionStore,
            WatchingSessionService service, WatchingSessionPayloadSender payloadSender) {
        super(sessionStore, StompConstants.SUB_WATCHING_CONTENT_PATTERN);
        this.service = service;
        this.payloadSender = payloadSender;
    }

    @Override
    protected void doHandle(UUID contentId, String email) {
        service.ensureWatchingContent(email, contentId);
        WatchingSessionResponse response = service.findSessionByWatcherEmail(email);
        service.delete(email);
        long watcherCount = service.getCurrentWatchingContentView(contentId);
        payloadSender.send(contentId,
                new WatchingSessionPayload(WatcherStatus.LEAVE, response, watcherCount));
    }
}
