package com.codeit.team5.mopl.watcher.command.unsubscribe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.codeit.team5.mopl.global.web.ws.stomp.base.BaseStompInboundChannelTest;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore.StompDestination;
import com.codeit.team5.mopl.watcher.provider.WatchingSessionPayloadSender;

class ContentChatUnsubscribeInboundChannelTest extends BaseStompInboundChannelTest {

    @Autowired
    private ContentChatUnsubscribeHandler handler;

    @MockitoBean
    private WatchingSessionPayloadSender payloadSender;

    @Test
    @DisplayName("STOMP UNSUBSCRIBE 메시지(chat)가 인입되면 ContentChatUnsubscribeHandler가 처리한다")
    void inboundContentChatUnsubscribeTest() {
        // given
        UUID testUserId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        String subscriptionId = "sub-001";
        String storedDestinationPattern = "/sub/contents/{id}/chat";
        // Unsubscribe 시에는 목적지(destination) 헤더가 없으므로 sessionStore에서 조회하도록 모킹
        when(sessionStore.getDestination(testUserId, subscriptionId))
                .thenReturn(Optional.of(new StompDestination(storedDestinationPattern, contentId)));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
        accessor.setSubscriptionId(subscriptionId);
        accessor.setUser(new UsernamePasswordAuthenticationToken(testUserId.toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        Message<byte[]> message =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // when
        clientInboundChannel.send(message);

        // then
        // 1. SessionStore 구독 해제 확인
        verify(sessionStore).unsubscribe(testUserId, subscriptionId);
        // ContentChatUnsubscribeHandler 내부 로직은 비어있음 (nothing to handle)
    }

    @Test
    @DisplayName("canHandle은 커맨드와 목적지가 일치할 때 true를 반환한다")
    void canHandle_True() {
        // given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
        accessor.setDestination("/sub/contents/1234/chat");

        // when
        boolean result = handler.canHandle(accessor);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("canHandle은 목적지가 불일치할 때 false를 반환한다")
    void canHandle_False_MismatchDestination() {
        // given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
        accessor.setDestination("/sub/contents/1234/watch"); // 불일치

        // when
        boolean result = handler.canHandle(accessor);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("canHandle은 커맨드가 불일치할 때 false를 반환한다")
    void canHandle_False_MismatchCommand() {
        // given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND); // 불일치
        accessor.setDestination("/sub/contents/1234/chat");

        // when
        boolean result = handler.canHandle(accessor);

        // then
        assertThat(result).isFalse();
    }
}
