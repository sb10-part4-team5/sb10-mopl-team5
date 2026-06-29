package com.codeit.team5.mopl.global.web.ws.stomp.handler;

import com.codeit.team5.mopl.auth.jwt.JwtTokenizer;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import io.jsonwebtoken.JwtException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class StompConnectHandler extends AbstractStompCommandHandler {

    private final JwtTokenizer jwtTokenizer;

    public StompConnectHandler(
            WebSocketSessionStore sessionStore,
            JwtTokenizer jwtTokenizer) {
        super(sessionStore, StompCommand.CONNECT);
        this.jwtTokenizer = jwtTokenizer;
    }

    @Override
    public void handle(StompHeaderAccessor accessor) {
        String token = parseToken(accessor);
        if (!StringUtils.hasText(token)) {
            throw new JwtException("token not found");
        }
        Authentication authentication = jwtTokenizer.getAuthentication(token);
        accessor.setUser(authentication);
        connectSession(authentication.getName());
    }
}
