package com.codeit.team5.mopl.watcher.listener;


import java.security.Principal;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;
import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore.StompDestination;
import com.codeit.team5.mopl.watcher.constant.WatcherStatus;
import com.codeit.team5.mopl.watcher.dto.payload.WatchingSessionPayload;
import com.codeit.team5.mopl.watcher.provider.WatchingSessionPayloadSender;
import com.codeit.team5.mopl.watcher.service.WatchingSessionCommandService;
import com.codeit.team5.mopl.watcher.service.WatchingSessionQueryService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WatchingSessionStompEventListener {

    private final WatchingSessionQueryService queryService;
    private final WatchingSessionCommandService commandService;
    private final WatchingSessionPayloadSender payloadSender;
    private final WebSocketSessionStore sessionStore;

    @EventListener
    public void handle(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor =
                StompHeaderAccessor.getAccessor(event.getMessage(), StompHeaderAccessor.class);
        if (accessor == null) {
            return;
        }
        Principal user = accessor.getUser();
        if (user == null) {
            return;
        }
        UUID userId = UUID.fromString(user.getName());
        String pattern = StompConstants.SUB_WATCHING_CONTENT.replace("{id}", "*");
        for (StompDestination dest : sessionStore.getAllDestination(userId)) {
            if (pattern.equals(dest.getPattern())) {
                UUID contentId = dest.targetId();
                WatchingSessionPayload payload =
                        queryService.getWatchingSessionPayload(userId, WatcherStatus.LEAVE);
                commandService.left(contentId, userId);
                payloadSender.send(contentId, payload);
                break;
            }
        }
        sessionStore.disconnect(userId);
    }

    @EventListener
    public void handle(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor =
                StompHeaderAccessor.getAccessor(event.getMessage(), StompHeaderAccessor.class);
        if (accessor == null || accessor.getSessionAttributes() == null) {
            return;
        }
        Principal principal = accessor.getUser();
        if (principal == null) {
            return;
        }
        UUID userId = UUID.fromString(principal.getName());
        String subId = accessor.getSubscriptionId();
        WatchingSessionPayload payload = (WatchingSessionPayload) accessor.getSessionAttributes()
                .get("%s/%s".formatted(userId, subId));
        if (payload == null) {
            return;
        }
        payloadSender.send(payload.response().content().id(), payload);
        accessor.getSessionAttributes().remove("%s/%s".formatted(userId, subId));
        sessionStore.unsubscribe(userId, subId);
    }

}
