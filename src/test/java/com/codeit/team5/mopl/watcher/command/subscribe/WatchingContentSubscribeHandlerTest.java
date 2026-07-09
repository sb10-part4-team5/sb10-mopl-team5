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
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore.StompDestination;
import com.codeit.team5.mopl.watcher.service.WatchingSessionCommandService;

@ExtendWith(MockitoExtension.class)
class WatchingContentSubscribeHandlerTest {

    @Mock
    private WebSocketSessionStore sessionStore;

    @Mock
    private WatchingSessionCommandService service;

    @InjectMocks
    private WatchingContentSubscribeHandler handler;

    @Test
    @DisplayName("handle 호출 시 세션을 생성하고 참여 메시지 브로드캐스트 및 sessionStore 구독이 발생한다")
    void handle_Success() {
        // Given
        UUID contentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String subscriptionId = "sub-0";
        String destination = "/sub/contents/" + contentId + "/watch";

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setSubscriptionId(subscriptionId);
        accessor.setUser(userId::toString);

        // When
        handler.handle(accessor);

        // Then
        verify(service).join(contentId, userId);
        verify(sessionStore).subscribe(userId, subscriptionId,
                new StompDestination("/sub/contents/{id}/watch", contentId));
    }

    @Test
    @DisplayName("커맨드가 다르면 canHandle은 false를 반환한다")
    void canHandle_False_WhenCommandIsDifferent() {
        // Given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/sub/contents/123/watch");

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
        accessor.setDestination("/sub/contents/123/chat"); // watch가 아님

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
        accessor.setDestination("/sub/contents/123/watch");

        // When
        boolean result = handler.canHandle(accessor);

        // Then
        assertThat(result).isTrue();
    }
}
