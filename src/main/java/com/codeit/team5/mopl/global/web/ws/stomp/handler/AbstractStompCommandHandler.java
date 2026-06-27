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
            String destination) {
        this.sessionStore = sessionStore;
        this.command = command;
        this.destinationPattern = destination.replace("{id}", "*");
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
        String email = accessor.getUser().getName();
        String storedDestination = getSessionDestination(email, subscriptionId);
        return storedDestination != null ? storedDestination : "";
    }

    protected void connectSession(String email) {
        sessionStore.connect(email);
    }

    protected void subscribeSession(String email, String subscriptionId, String destination) {
        sessionStore.subscribe(email, subscriptionId, destination);
    }

    protected void unsubscribeSession(String email, String subscriptionId) {
        sessionStore.unsubscribe(email, subscriptionId);
    }

    protected String getSessionDestination(String email, String subscriptionId) {
        return sessionStore.getDestination(email, subscriptionId);
    }

    protected UUID getTargetId(String destination) {
        String id = matcher.extractPathWithinPattern(destinationPattern, destination);
        return UUID.fromString(id);
    }

    protected String parseToken(
            org.springframework.messaging.simp.stomp.StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (org.springframework.util.StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return "";
    }

    private boolean matchCommand(StompCommand command) {
        return this.command.equals(command);
    }
}
