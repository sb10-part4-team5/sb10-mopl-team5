package com.codeit.team5.mopl.global.web.ws.config;

import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

@Configuration
@EnableWebSocketSecurity
public class WebSocketSecurityConfig {

    @Bean
    public ChannelInterceptor csrfChannelInterceptor() {
        return new ChannelInterceptor() {
        };
    }

    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager(
            MessageMatcherDelegatingAuthorizationManager.Builder messages) {

        String subWatchingContentChatPattern = StompConstants.SUB_WATCHING_CONTENT_CHAT.replace(
                "{id}", "*");
        String subWatchingContentPattern = StompConstants.SUB_WATCHING_CONTENT.replace("{id}", "*");
        String pubWatchingContentChatPattern =
                StompConstants.PUB_PREFIX + StompConstants.PUB_WATCHING_CONTENT_CHAT;
        return messages
                .simpSubscribeDestMatchers(subWatchingContentChatPattern, subWatchingContentPattern)
                .authenticated()
                .simpDestMatchers(pubWatchingContentChatPattern).authenticated()
                .anyMessage().permitAll()
                .build();
    }
}
