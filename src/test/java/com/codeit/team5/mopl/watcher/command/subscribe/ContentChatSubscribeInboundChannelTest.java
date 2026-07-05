package com.codeit.team5.mopl.watcher.command.subscribe;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import com.codeit.team5.mopl.global.web.ws.stomp.handler.StompCommandHandler;
import com.codeit.team5.mopl.global.web.ws.stomp.interceptor.StompInterceptor;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import com.codeit.team5.mopl.watcher.service.WatchingSessionQueryService;

@ExtendWith(MockitoExtension.class)
class ContentChatSubscribeInboundChannelTest {

    @Mock
    private WebSocketSessionStore sessionStore;

    @Mock
    private WatchingSessionQueryService watchingSessionQueryService;

    @Mock
    private MessageChannel messageChannel;

    private StompInterceptor stompInterceptor;

    @BeforeEach
    void setUp() {
        ContentChatSubscribeHandler handler =
                new ContentChatSubscribeHandler(sessionStore, watchingSessionQueryService);
        stompInterceptor = new StompInterceptor(List.of((StompCommandHandler) handler));
    }

    @Test
    @DisplayName("STOMP SUBSCRIBE /sub/contents/{contentId}/chat 요청 시 ensureWatchingContent 호출")
    void testSubscribeContentChatFlow() {
        // Given
        UUID testUserId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        String destination = "/sub/contents/" + contentId + "/chat";
        String subscriptionId = "sub-0";

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setSubscriptionId(subscriptionId);
        accessor.setUser(() -> testUserId.toString());
        Message<byte[]> message =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // When
        stompInterceptor.preSend(message, messageChannel);

        // Then
        verify(watchingSessionQueryService).ensureWatchingContent(testUserId, contentId);
        verify(sessionStore).subscribe(testUserId, subscriptionId, destination);
    }

    @Test
    @DisplayName("다른 목적지로 SUBSCRIBE 시 핸들러 동작 안 함")
    void testSubscribeOtherDestination() {
        // Given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/sub/contents/123/watch");
        accessor.setSubscriptionId("sub-1");
        Message<byte[]> message =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // When
        stompInterceptor.preSend(message, messageChannel);

        // Then
        verifyNoInteractions(watchingSessionQueryService);
    }
}
