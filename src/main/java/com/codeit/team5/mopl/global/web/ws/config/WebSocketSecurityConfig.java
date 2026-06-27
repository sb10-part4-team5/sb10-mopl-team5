package com.codeit.team5.mopl.global.web.ws.config;

import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

@Configuration
@EnableWebSocketSecurity
public class WebSocketSecurityConfig {

    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager(
            MessageMatcherDelegatingAuthorizationManager.Builder messages) {
        return messages
                .simpSubscribeDestMatchers(StompConstants.SUB_WATCHING_CONTENT_CHAT_PATTERN,
                        StompConstants.SUB_WATCHING_CONTENT_PATTERN).authenticated()
                .simpDestMatchers(StompConstants.PUB_WATCHING_CONTENT_CHAT).authenticated()
                .anyMessage().permitAll()
                .build();
    }
}
