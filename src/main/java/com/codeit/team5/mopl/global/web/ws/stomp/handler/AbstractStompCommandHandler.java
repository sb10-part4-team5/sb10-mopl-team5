package com.codeit.team5.mopl.global.web.ws.stomp.handler;

import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore.StompDestination;
import java.util.Optional;
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
        if (!matchCommand(command)) {
            return false;
        }
        String incomingDestination = accessor.getDestination();
        // 1. 목적지가 명시된 경우 (SUBSCRIBE, SEND 등) -> 기존처럼 AntPathMatcher 사용
        if (StringUtils.hasText(incomingDestination)) {
            return matchDestination(incomingDestination);
        }
        // 2. 목적지가 없는 경우 (UNSUBSCRIBE 등) -> 세션 스토어의 패턴 활용
        String subscriptionId = accessor.getSubscriptionId();
        if (accessor.getUser() == null || subscriptionId == null) {
            return !StringUtils.hasText(this.destinationPattern); // CONNECT 등
        }
        UUID userId = UUID.fromString(accessor.getUser().getName());
        Optional<StompDestination> storedDestination = getSessionDestination(userId,
            subscriptionId);
        // 저장된 객체가 있다면, 핸들러가 가진 패턴(this.destinationPattern)과
        // storedDestination의 패턴을 비교하여 매칭을 완료함
        return storedDestination.map(StompDestination::destination)
            .map(this.destinationPattern::equals).orElse(false);
    }

    protected boolean matchDestination(String destination) {
        if (!StringUtils.hasText(this.destinationPattern)) {
            return true; // 목적지 검사가 필요 없는 핸들러 (예: CONNECT)
        }
        return matcher.match(this.destinationPattern, destination);
    }

    protected void connectSession(UUID userId) {
        sessionStore.connect(userId);
    }

    protected void subscribeSession(UUID userId, String subscriptionId,
        StompDestination destination) {
        sessionStore.subscribe(userId, subscriptionId, destination);
    }

    protected void unsubscribeSession(UUID userId, String subscriptionId) {
        sessionStore.unsubscribe(userId, subscriptionId);
    }

    protected Optional<StompDestination> getSessionDestination(UUID userId, String subscriptionId) {
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

