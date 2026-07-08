package com.codeit.team5.mopl.global.web.ws.stomp.handler;

import java.util.UUID;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;

public abstract class AbstractStompSubscribeHandler extends AbstractStompCommandHandler {

    protected AbstractStompSubscribeHandler(
            WebSocketSessionStore sessionStore,
            String destinationPattern) {
        super(sessionStore, StompCommand.SUBSCRIBE, destinationPattern);
    }

    @Override
    public void handle(StompHeaderAccessor accessor) {
        String subscriptionId = accessor.getSubscriptionId();
        String destination = accessor.getDestination();
        UUID userId = UUID.fromString(accessor.getUser().getName());
        UUID targetId = getTargetId(destination);
        doHandle(targetId, userId);
        subscribeSession(userId, subscriptionId, destination);
    }

    protected abstract void doHandle(UUID targetId, UUID userId);

}
