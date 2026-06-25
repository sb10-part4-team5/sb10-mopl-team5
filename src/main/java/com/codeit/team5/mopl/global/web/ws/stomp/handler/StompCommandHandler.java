package com.codeit.team5.mopl.global.web.ws.stomp.handler;

import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

public interface StompCommandHandler {

    boolean canHandle(StompCommand command);

    void handle(StompHeaderAccessor accessor);
}
