package com.codeit.team5.mopl.watcher.command.subscribe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import com.codeit.team5.mopl.watcher.service.WatchingSessionQueryService;

@ExtendWith(MockitoExtension.class)
class ContentChatSubscribeHandlerTest {

    @Mock
    private WebSocketSessionStore sessionStore;

    @Mock
    private WatchingSessionQueryService service;

    @InjectMocks
    private ContentChatSubscribeHandler handler;

    @Test
    @DisplayName("handle 호출 시 service의 ensureWatchingContent 및 sessionStore.subscribe가 호출된다")
    void handle_Success() {
        // Given
        UUID contentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String subscriptionId = "sub-0";
        String destination = "/sub/contents/" + contentId + "/chat";

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setSubscriptionId(subscriptionId);
        accessor.setUser(() -> userId.toString());

        // When
        handler.handle(accessor);

        // Then
        verify(service).ensureWatchingContent(contentId, userId);
        verify(sessionStore).subscribe(userId, subscriptionId, destination);
    }

    @Test
    @DisplayName("커맨드가 다르면 canHandle은 false를 반환한다")
    void canHandle_False_WhenCommandIsDifferent() {
        // Given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/sub/contents/123/chat");

        // When
        boolean result = handler.canHandle(accessor);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("목적지가 다르면 canHandle은 false를 반환한다")
    void canHandle_False_WhenDestinationIsDifferent() {
        // Given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/sub/contents/123/watch"); // chat이 아님

        // When
        boolean result = handler.canHandle(accessor);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("커맨드와 목적지가 모두 일치하면 canHandle은 true를 반환한다")
    void canHandle_True_WhenMatch() {
        // Given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/sub/contents/123/chat");

        // When
        boolean result = handler.canHandle(accessor);

        // Then
        assertThat(result).isTrue();
    }
}
