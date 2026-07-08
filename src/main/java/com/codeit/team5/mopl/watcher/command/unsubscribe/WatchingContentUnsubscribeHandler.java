package com.codeit.team5.mopl.watcher.command.unsubscribe;

import java.util.UUID;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;
import com.codeit.team5.mopl.global.web.ws.stomp.handler.AbstractStompUnsubscribeHandler;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import com.codeit.team5.mopl.watcher.constant.WatcherStatus;
import com.codeit.team5.mopl.watcher.dto.payload.WatchingSessionPayload;
import com.codeit.team5.mopl.watcher.service.WatchingSessionCommandService;
import com.codeit.team5.mopl.watcher.service.WatchingSessionQueryService;

@Component
public class WatchingContentUnsubscribeHandler extends AbstractStompUnsubscribeHandler {

    private final WatchingSessionCommandService commandService;
    private final WatchingSessionQueryService queryService;


    public WatchingContentUnsubscribeHandler(WebSocketSessionStore sessionStore,
            WatchingSessionCommandService commandService,
            WatchingSessionQueryService queryService) {
        super(sessionStore, StompConstants.SUB_WATCHING_CONTENT);
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @Override
    protected void doHandle(UUID contentId, UUID userId, StompHeaderAccessor accessor) {
        WatchingSessionPayload payload =
                queryService.getWatchingSessionPayload(userId, WatcherStatus.LEAVE);
        commandService.left(contentId, userId);
        if (accessor.getSessionAttributes() != null) {
            accessor.getSessionAttributes()
                    .put("%s/%s".formatted(userId, accessor.getSubscriptionId()), payload);
        }
    }
}
