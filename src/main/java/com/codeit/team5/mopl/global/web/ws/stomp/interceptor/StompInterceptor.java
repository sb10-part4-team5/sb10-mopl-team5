package com.codeit.team5.mopl.global.web.ws.stomp.interceptor;

import java.util.List;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import com.codeit.team5.mopl.global.web.ws.stomp.handler.StompCommandHandler;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StompInterceptor implements ChannelInterceptor {

    private final List<StompCommandHandler> handlers;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        for (StompCommandHandler handler : handlers) {
            if (handler.canHandle(accessor)) {
                handler.handle(accessor);
                break;
            }
        }
        return message;
    }
}
