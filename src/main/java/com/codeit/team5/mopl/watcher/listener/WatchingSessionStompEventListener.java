package com.codeit.team5.mopl.watcher.listener;


import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore.StompDestination;
import com.codeit.team5.mopl.watcher.constant.WatcherStatus;
import com.codeit.team5.mopl.watcher.dto.payload.WatchingSessionPayload;
import com.codeit.team5.mopl.watcher.dto.payload.WatchingSessionRedisMessage;
import com.codeit.team5.mopl.watcher.service.WatchingSessionCommandService;
import com.codeit.team5.mopl.watcher.service.WatchingSessionQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchingSessionStompEventListener {

    private final WatchingSessionQueryService queryService;
    private final WatchingSessionCommandService commandService;
    private final WebSocketSessionStore sessionStore;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

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
        try {
            handleUserDisconnect(userId);
        } finally {
            sessionStore.disconnect(userId);
        }
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
        publishToRedis(payload.response().content().id(), payload);
        accessor.getSessionAttributes().remove("%s/%s".formatted(userId, subId));
    }

    private void handleUserDisconnect(UUID userId) {
        String watchingContentPattern = StompConstants.SUB_WATCHING_CONTENT.replace("{id}", "*");
        for (StompDestination destination : sessionStore.getAllDestination(userId)) {
            if (isWatchingContentDestination(destination, watchingContentPattern)) {
                UUID contentId = destination.targetId();
                leaveWatchingSession(contentId, userId);
                break;
            }
        }
    }

    private boolean isWatchingContentDestination(StompDestination destination, String pattern) {
        return pattern.equals(destination.getPattern());
    }

    private void leaveWatchingSession(UUID contentId, UUID userId) {
        WatchingSessionPayload payload =
                queryService.getWatchingSessionPayload(userId, WatcherStatus.LEAVE);
        commandService.left(contentId, userId);
        publishToRedis(contentId, payload);
    }

    private void publishToRedis(UUID contentId, WatchingSessionPayload payload) {
        try {
            String message = objectMapper.writeValueAsString(
                    new WatchingSessionRedisMessage(contentId, payload));
            redisTemplate.convertAndSend("watching-session-topic", message);
        } catch (Exception e) {
            log.error("{}", e);
        }
    }
}
