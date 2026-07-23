package com.codeit.team5.mopl.watcher.command.unsubscribe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import com.codeit.team5.mopl.watcher.constant.WatcherStatus;
import com.codeit.team5.mopl.watcher.dto.payload.WatchingSessionPayload;
import com.codeit.team5.mopl.watcher.dto.response.WatchingSessionResponse;
import com.codeit.team5.mopl.watcher.service.WatchingSessionCommandService;
import com.codeit.team5.mopl.watcher.service.WatchingSessionQueryService;

@ExtendWith(MockitoExtension.class)
class WatchingContentUnsubscribeHandlerTest {

    @Mock
    private WebSocketSessionStore sessionStore;

    @Mock
    private WatchingSessionCommandService service;

    @Mock
    private WatchingSessionQueryService queryService;

    @InjectMocks
    private WatchingContentUnsubscribeHandler handler;

    @Test
    @DisplayName("handle 호출 시 세션을 삭제하고 퇴장 메시지 브로드캐스트 및 sessionStore 구독 해제가 발생한다")
    void handle_Success() {
        // Given
        UUID contentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String subscriptionId = "sub-0";
        String destination = "/sub/contents/" + contentId + "/watch";

        WatchingSessionResponse response = WatchingSessionResponse.builder().build();
        long watchCount = 4L;
        WatchingSessionPayload payload =
                new WatchingSessionPayload(WatcherStatus.LEAVE, response, watchCount);

        given(queryService.getWatchingSessionPayload(contentId, userId, WatcherStatus.LEAVE))
                .willReturn(payload);
        when(sessionStore.getDestination(userId, subscriptionId)).thenReturn(java.util.Optional.of(
                new WebSocketSessionStore.StompDestination("/sub/contents/{id}/watch", contentId)));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
        accessor.setSubscriptionId(subscriptionId);
        accessor.setUser(() -> userId.toString());
        accessor.setSessionAttributes(new java.util.HashMap<>());

        // When
        handler.handle(accessor);

        // Then
        verify(service).left(contentId, userId);
        org.assertj.core.api.Assertions
                .assertThat(accessor.getSessionAttributes().get(userId + "/" + subscriptionId))
                .isEqualTo(payload);
        verify(sessionStore).unsubscribe(userId, subscriptionId);
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
        UUID userId = UUID.randomUUID();
        String subscriptionId = "sub-0";
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
        accessor.setSubscriptionId(subscriptionId);
        accessor.setUser(() -> userId.toString());

        when(sessionStore.getDestination(userId, subscriptionId))
                .thenReturn(java.util.Optional.of(new WebSocketSessionStore.StompDestination(
                        "/sub/contents/{id}/chat", java.util.UUID.randomUUID())));

        // When
        boolean result = handler.canHandle(accessor);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("커맨드와 목적지가 모두 일치하면 canHandle은 true를 반환한다")
    void canHandle_True_WhenMatch() {
        // Given
        UUID userId = UUID.randomUUID();
        String subscriptionId = "sub-0";
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
        accessor.setSubscriptionId(subscriptionId);
        accessor.setUser(() -> userId.toString());

        when(sessionStore.getDestination(userId, subscriptionId))
                .thenReturn(java.util.Optional.of(new WebSocketSessionStore.StompDestination(
                        "/sub/contents/{id}/watch", java.util.UUID.randomUUID())));

        // When
        boolean result = handler.canHandle(accessor);

        // Then
        assertThat(result).isTrue();
    }
}
