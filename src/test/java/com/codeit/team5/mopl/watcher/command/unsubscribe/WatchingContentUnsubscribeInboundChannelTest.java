package com.codeit.team5.mopl.watcher.command.unsubscribe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import com.codeit.team5.mopl.watcher.dto.payload.WatchingSessionPayload;
import com.codeit.team5.mopl.watcher.dto.response.WatchingSessionResponse;
import com.codeit.team5.mopl.watcher.provider.WatchingSessionPayloadSender;

class WatchingContentUnsubscribeInboundChannelTest extends BaseStompInboundChannelTest {

    @Autowired
    private WatchingContentUnsubscribeHandler handler;

    @MockitoBean
    private WatchingSessionPayloadSender payloadSender;

    @Test
    @DisplayName("STOMP UNSUBSCRIBE 메시지(watch)가 인입되면 WatchingContentUnsubscribeHandler가 처리한다")
    void inboundWatchingContentUnsubscribeTest() {
        // given
        UUID testUserId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        String subscriptionId = "sub-002";
        String storedDestination = "/sub/contents/" + contentId + "/watch";

        // Unsubscribe 목적지 조회 모킹
        when(sessionStore.getDestination(testUserId, subscriptionId)).thenReturn(storedDestination);

        WatchingSessionResponse fakeResponse =
                new WatchingSessionResponse(testUserId, null, null, null);
        when(watchingSessionService.findSessionByWatchId(testUserId)).thenReturn(fakeResponse);
        when(watchingSessionService.getCurrentWatchingContentView(contentId)).thenReturn(4L);

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

        // 2. Service.delete() 등 내부 로직 호출 확인
        verify(watchingSessionService).ensureWatchingContent(testUserId, contentId);
        verify(watchingSessionService).delete(testUserId);

        // 3. PayloadSender 이벤트 발행 확인
        verify(payloadSender).send(eq(contentId), any(WatchingSessionPayload.class));
    }

    @Test
    @DisplayName("STOMP UNSUBSCRIBE 메시지에 연결된 구독 목적지가 유효한 UUID가 아니면 IllegalArgumentException 예외가 발생한다_실패")
    void inboundUnsubscribeTest_Fail_InvalidDestinationId() {
        // given
        UUID testUserId = UUID.randomUUID();
        String subscriptionId = "sub-invalid";
        String storedDestination = "/sub/contents/invalid-uuid-string/watch";

        when(sessionStore.getDestination(testUserId, subscriptionId)).thenReturn(storedDestination);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
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
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
        accessor.setDestination("/sub/contents/1234/watch");

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
        accessor.setDestination("/sub/contents/1234/chat"); // 불일치

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
        accessor.setDestination("/sub/contents/1234/watch");

        // when
        boolean result = handler.canHandle(accessor);

        // then
        assertThat(result).isFalse();
    }
}
