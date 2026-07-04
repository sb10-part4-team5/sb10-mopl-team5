package com.codeit.team5.mopl.watcher.command.subscribe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import java.util.List;
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
import com.codeit.team5.mopl.watcher.provider.WatchingSessionPayloadSender;

class ContentChatSubscribeInboundChannelTest extends BaseStompInboundChannelTest {

    @Autowired
    private ContentChatSubscribeHandler handler;

    @MockitoBean
    private WatchingSessionPayloadSender payloadSender;

    @Test
    @DisplayName("STOMP SUBSCRIBE 메시지(chat)가 인입되면 ContentChatSubscribeHandler가 처리한다")
    void inboundContentChatSubscribeTest() {
        // given
        UUID testUserId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        String subscriptionId = "sub-001";
        String destination = "/sub/contents/" + contentId + "/chat";

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setSubscriptionId(subscriptionId);
        accessor.setUser(new UsernamePasswordAuthenticationToken(testUserId.toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        Message<byte[]> message =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // when
        clientInboundChannel.send(message);

        // then
        // 1. WebSocketSessionStore.subscribe() 호출 확인 (AbstractStompSubscribeHandler 에서 수행)
        verify(sessionStore).subscribe(testUserId, subscriptionId, destination);
        // 2. WatchingSessionService.ensureWatchingContent() 호출 확인 (ContentChatSubscribeHandler 내부
        // 로직)
        verify(watchingSessionService).ensureWatchingContent(testUserId, contentId);
    }

    @Test
    @DisplayName("STOMP SUBSCRIBE 메시지의 목적지 ID가 유효한 UUID가 아니면 IllegalArgumentException 예외가 발생한다_실패")
    void inboundSubscribeTest_Fail_InvalidDestinationId() {
        // given
        UUID testUserId = UUID.randomUUID();
        String subscriptionId = "sub-invalid";
        String destination = "/sub/contents/invalid-uuid-string/chat";

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setSubscriptionId(subscriptionId);
        accessor.setUser(new UsernamePasswordAuthenticationToken(testUserId.toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        Message<byte[]> message =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // when & then
        assertThatThrownBy(() -> clientInboundChannel.send(message)).cause()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid destination id format");
    }

    @Test
    @DisplayName("canHandle은 커맨드와 목적지가 일치할 때 true를 반환한다")
    void canHandle_True() {
        // given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
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
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
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
