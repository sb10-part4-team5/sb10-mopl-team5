package com.codeit.team5.mopl.global.web.ws.stomp.handler;

import com.codeit.team5.mopl.global.web.ws.stomp.handler.command.subscribe.StompSubscribeHandler;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompCommandHandlerImpl implements StompCommandHandler {
    private final List<StompSubscribeHandler> handlers;

    @Override
    public boolean canHandle(StompCommand command) {
        return StompCommand.SUBSCRIBE.equals(command);
    }

    @Override
    public void handle(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        for (StompSubscribeHandler handler: handlers) {
            if (handler.canSupport(destination)) {
                handler.validate(accessor);
                break;
            }
        }
    }
}
