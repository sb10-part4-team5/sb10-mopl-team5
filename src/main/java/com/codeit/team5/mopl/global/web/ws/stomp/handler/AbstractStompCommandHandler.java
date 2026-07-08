package com.codeit.team5.mopl.global.web.ws.stomp.handler;

import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import java.util.UUID;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

public abstract class AbstractStompCommandHandler implements StompCommandHandler {

    protected static final AntPathMatcher matcher = new AntPathMatcher();
    private final WebSocketSessionStore sessionStore;
    protected final StompCommand command;
    protected final String destinationPattern;

    protected AbstractStompCommandHandler(WebSocketSessionStore sessionStore,
            StompCommand command) {
        this.sessionStore = sessionStore;
        this.command = command;
        this.destinationPattern = "";
    }

    protected AbstractStompCommandHandler(WebSocketSessionStore sessionStore, StompCommand command,
            String destinationPattern) {
        this.sessionStore = sessionStore;
        this.command = command;
        this.destinationPattern = destinationPattern;
    }

    @Override
    public boolean canHandle(StompHeaderAccessor accessor) {
        StompCommand command = accessor.getCommand();
        String destination = getDestination(accessor);
        return matchCommand(command) && matchDestination(destination);
    }

    protected boolean matchDestination(String destination) {
        if (!StringUtils.hasText(this.destinationPattern)) {
            return true; // 목적지 검사가 필요 없는 핸들러 (예: CONNECT)
        }
        return matcher.match(this.destinationPattern, destination);
    }

    protected String getDestination(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (StringUtils.hasText(destination)) {
            return destination;
        }
        String subscriptionId = accessor.getSubscriptionId();
        if (accessor.getUser() == null || subscriptionId == null) {
            return "";
        }
        UUID userId = UUID.fromString(accessor.getUser().getName());
        String storedDestination = getSessionDestination(userId, subscriptionId);
        return storedDestination != null ? storedDestination : "";
    }

    protected void connectSession(UUID userId) {
        sessionStore.connect(userId);
    }

    protected void subscribeSession(UUID userId, String subscriptionId, String destination) {
        sessionStore.subscribe(userId, subscriptionId, destination);
    }

    protected void unsubscribeSession(UUID userId, String subscriptionId) {
        sessionStore.unsubscribe(userId, subscriptionId);
    }

    protected String getSessionDestination(UUID userId, String subscriptionId) {
        return sessionStore.getDestination(userId, subscriptionId);
    }

    protected UUID getTargetId(String destination) {
        String id = matcher.extractUriTemplateVariables(destinationPattern, destination).get("id");
        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException("Invalid destination: missing id");
        }
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid destination id format", e);
        }
    }

    protected String parseToken(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return "";
    }

    private boolean matchCommand(StompCommand command) {
        return this.command.equals(command);
    }
}
