package com.codeit.team5.mopl.watcher.command.unsubscribe;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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
import com.codeit.team5.mopl.watcher.constant.WatcherStatus;
import com.codeit.team5.mopl.watcher.dto.payload.WatchingSessionPayload;
import com.codeit.team5.mopl.watcher.dto.response.WatchingSessionResponse;
import com.codeit.team5.mopl.watcher.provider.WatchingSessionPayloadSender;
import com.codeit.team5.mopl.watcher.service.WatchingSessionCommandService;

@ExtendWith(MockitoExtension.class)
class WatchingContentUnsubscribeInboundChannelTest {

        @Mock
        private WebSocketSessionStore sessionStore;

        @Mock
        private WatchingSessionCommandService watchingSessionCommandService;

        @Mock
        private WatchingSessionPayloadSender payloadSender;

        @Mock
        private MessageChannel messageChannel;

        private StompInterceptor stompInterceptor;

        @BeforeEach
        void setUp() {
                WatchingContentUnsubscribeHandler handler = new WatchingContentUnsubscribeHandler(
                                sessionStore, watchingSessionCommandService, payloadSender);
                stompInterceptor = new StompInterceptor(List.of((StompCommandHandler) handler));
        }

        @Test
        @DisplayName("STOMP UNSUBSCRIBE 요청 시 세션 삭제 및 브로드캐스트 수행")
        void testUnsubscribeWatchingContentFlow() {
                // Given
                UUID testUserId = UUID.randomUUID();
                UUID contentId = UUID.randomUUID();
                String destination = "/sub/contents/" + contentId + "/watch";
                String subscriptionId = "sub-0";

                StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
                accessor.setSubscriptionId(subscriptionId);
                accessor.setUser(() -> testUserId.toString());
                Message<byte[]> message = MessageBuilder.createMessage(new byte[0],
                                accessor.getMessageHeaders());

                when(sessionStore.getDestination(testUserId, subscriptionId))
                                .thenReturn(destination);

                WatchingSessionResponse fakeResponse = WatchingSessionResponse.builder().build();
                WatchingSessionPayload payload =
                                new WatchingSessionPayload(WatcherStatus.LEAVE, fakeResponse, 4L);

                when(watchingSessionCommandService.left(testUserId)).thenReturn(payload);

                // When
                stompInterceptor.preSend(message, messageChannel);

                // Then
                verify(watchingSessionCommandService).left(testUserId);
                verify(payloadSender).send(eq(contentId), eq(payload));
                verify(sessionStore).unsubscribe(testUserId, subscriptionId);
        }

        @Test
        @DisplayName("다른 목적지로 UNSUBSCRIBE 시 핸들러 동작 안 함")
        void testUnsubscribeOtherDestination() {
                // Given
                UUID testUserId = UUID.randomUUID();
                String subscriptionId = "sub-1";
                StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
                accessor.setSubscriptionId(subscriptionId);
                accessor.setUser(() -> testUserId.toString());
                Message<byte[]> message = MessageBuilder.createMessage(new byte[0],
                                accessor.getMessageHeaders());

                when(sessionStore.getDestination(testUserId, subscriptionId))
                                .thenReturn("/sub/contents/123/chat");

                // When
                stompInterceptor.preSend(message, messageChannel);

                // Then
                verifyNoInteractions(watchingSessionCommandService);
                verifyNoInteractions(payloadSender);
        }
}
