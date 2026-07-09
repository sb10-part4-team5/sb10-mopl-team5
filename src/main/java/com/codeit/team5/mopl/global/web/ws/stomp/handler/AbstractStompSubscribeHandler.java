package com.codeit.team5.mopl.global.web.ws.stomp.handler;

import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore.StompDestination;
import java.util.UUID;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;

public abstract class AbstractStompSubscribeHandler extends AbstractStompCommandHandler {

    protected AbstractStompSubscribeHandler(WebSocketSessionStore sessionStore,
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
        // StompDestination의 첫 번째 인자는 패턴이어야 하므로 this.destinationPattern 사용
        StompDestination stompDestination = new StompDestination(this.destinationPattern, targetId);
        subscribeSession(userId, subscriptionId, stompDestination);
    }

    protected abstract void doHandle(UUID targetId, UUID userId);

}

