package com.codeit.team5.mopl.watcher.command.unsubscribe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.messaging.simp.stomp.StompCommand.SUBSCRIBE;
import static org.springframework.messaging.simp.stomp.StompCommand.UNSUBSCRIBE;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;

@ExtendWith(MockitoExtension.class)
class ContentChatUnsubscribeHandlerTest {

    @Mock
    private WebSocketSessionStore sessionStore;

    @InjectMocks
    private ContentChatUnsubscribeHandler handler;

    @Test
    @DisplayName("doHandle 호출 시 아무 동작도 하지 않는다_성공")
    void doHandle_Success() {
        // Given
        UUID contentId = UUID.randomUUID();
        UUID email = UUID.randomUUID();
        StompHeaderAccessor accessor = StompHeaderAccessor.create(UNSUBSCRIBE);

        // When
        handler.doHandle(contentId, email, accessor);

        // Then
        verifyNoInteractions(sessionStore);
    }

    @Test
    @DisplayName("커맨드가 다르면 canHandle은 false를 반환한다")
    void canHandle_False_WhenCommandIsDifferent() {
        // Given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(SUBSCRIBE);
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
        StompHeaderAccessor accessor = StompHeaderAccessor.create(UNSUBSCRIBE);
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
        StompHeaderAccessor accessor = StompHeaderAccessor.create(UNSUBSCRIBE);
        accessor.setDestination("/sub/contents/123/chat");

        // When
        boolean result = handler.canHandle(accessor);

        // Then
        assertThat(result).isTrue();
    }
}
