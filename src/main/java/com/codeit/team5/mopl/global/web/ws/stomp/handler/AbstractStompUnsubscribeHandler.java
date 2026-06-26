package com.codeit.team5.mopl.global.web.ws.stomp.handler;

import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import java.util.UUID;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

public abstract class AbstractStompUnsubscribeHandler extends AbstractStompCommandHandler {

    public AbstractStompUnsubscribeHandler(WebSocketSessionStore sessionStore, String destination) {
        super(sessionStore, StompCommand.UNSUBSCRIBE, destination);
    }

    @Override
    public void handle(StompHeaderAccessor accessor) {
        String email = accessor.getUser().getName();
        String subscriptionId = accessor.getSubscriptionId();
        String destination = getDestination(accessor); // 부모 클래스의 만능 메서드 활용!
        UUID targetId = getTargetId(destination);
        doHandle(targetId, email);
        unsubscribeSession(email, subscriptionId);
    }

    protected abstract void doHandle(UUID targetId, String email);
}
