package com.codeit.team5.mopl.global.web.ws.stomp.handler;

import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import java.util.UUID;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

public abstract class AbstractStompSubscribeHandler extends AbstractStompCommandHandler {

    protected AbstractStompSubscribeHandler(
            WebSocketSessionStore sessionStore,
            String destination) {
        super(sessionStore, StompCommand.SUBSCRIBE, destination);
    }

    @Override
    public void handle(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        UUID targetId = getTargetId(destination);
        String email = accessor.getUser().getName();
        doHandle(targetId, email);
        String subscriptionId = accessor.getSubscriptionId();
        subscribeSession(email, subscriptionId, destination);
    }

    protected abstract void doHandle(UUID targetId, String email);

}
