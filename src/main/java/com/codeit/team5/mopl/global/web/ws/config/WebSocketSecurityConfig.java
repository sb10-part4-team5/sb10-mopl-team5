package com.codeit.team5.mopl.global.web.ws.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;

@Configuration
@EnableWebSocketSecurity
public class WebSocketSecurityConfig {

    // JWT 헤더 인증은 CSRF 공격이 성립하지 않으므로, no-op 빈으로 기본 XorCsrfChannelInterceptor를 대체해 WS CSRF 검증을 끈다. (제거 시 CONNECT 전부 거부됨)
    @Bean
    public ChannelInterceptor csrfChannelInterceptor() {
        return new ChannelInterceptor() {
        };
    }

    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager(
            MessageMatcherDelegatingAuthorizationManager.Builder messages) {

        String subWatchingContentChatPattern =
                StompConstants.SUB_WATCHING_CONTENT_CHAT.replace("{id}", "*");
        String subWatchingContentPattern = StompConstants.SUB_WATCHING_CONTENT.replace("{id}", "*");
        String pubWatchingContentChatPattern =
                StompConstants.PUB_PREFIX + StompConstants.PUB_WATCHING_CONTENT_CHAT;
        return messages
                .simpSubscribeDestMatchers(subWatchingContentChatPattern, subWatchingContentPattern)
                .authenticated().simpDestMatchers(pubWatchingContentChatPattern).authenticated()
                .anyMessage().permitAll().build();
    }
}
