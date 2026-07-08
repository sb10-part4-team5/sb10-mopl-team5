package com.codeit.team5.mopl.global.web.ws.stomp.handler;

import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import java.util.Objects;
import java.util.UUID;
import java.security.Principal;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

public abstract class AbstractStompUnsubscribeHandler extends AbstractStompCommandHandler {

    protected AbstractStompUnsubscribeHandler(
            WebSocketSessionStore sessionStore,
            String destinationPattern) {
        super(sessionStore, StompCommand.UNSUBSCRIBE, destinationPattern);
    }

    @Override
    public void handle(StompHeaderAccessor accessor) {
        Principal principal = Objects.requireNonNull(accessor.getUser());
        String subscriptionId = accessor.getSubscriptionId();
        UUID userId = UUID.fromString(principal.getName());
        String destination = getSessionDestination(userId, subscriptionId);
        if (destination != null) {
            UUID targetId = getTargetId(destination);
            doHandle(targetId, userId);
            unsubscribeSession(userId, subscriptionId);
        }
    }

    protected abstract void doHandle(UUID targetId, UUID userId);

}

