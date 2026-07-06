package com.codeit.team5.mopl.global.web.ws.stomp.handler;

import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

public interface StompCommandHandler {

    boolean canHandle(StompHeaderAccessor accessor);

    void handle(StompHeaderAccessor accessor);
}
