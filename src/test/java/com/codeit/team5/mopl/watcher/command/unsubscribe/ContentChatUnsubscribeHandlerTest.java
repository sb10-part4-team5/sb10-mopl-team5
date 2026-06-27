package com.codeit.team5.mopl.watcher.command.unsubscribe;

import static org.mockito.Mockito.verifyNoInteractions;

import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        String email = "test@test.com";

        // When
        handler.doHandle(contentId, email);

        // Then
        // 아무것도 하지 않아야 함 (NoException 발생)
    }

    @Test
    @DisplayName("커맨드가 다르면 canHandle은 false를 반환한다")
    void canHandle_False_WhenCommandIsDifferent() {
        // Given
        org.springframework.messaging.simp.stomp.StompHeaderAccessor accessor = 
            org.springframework.messaging.simp.stomp.StompHeaderAccessor.create(org.springframework.messaging.simp.stomp.StompCommand.SUBSCRIBE);
        accessor.setDestination("/sub/contents/123/chat");

        // When
        boolean result = handler.canHandle(accessor);

        // Then
        org.assertj.core.api.Assertions.assertThat(result).isFalse();
    }

    @Test
    @DisplayName("목적지가 다르면 canHandle은 false를 반환한다")
    void canHandle_False_WhenDestinationIsDifferent() {
        // Given
        org.springframework.messaging.simp.stomp.StompHeaderAccessor accessor = 
            org.springframework.messaging.simp.stomp.StompHeaderAccessor.create(org.springframework.messaging.simp.stomp.StompCommand.UNSUBSCRIBE);
        accessor.setDestination("/sub/contents/123/watch"); // chat이 아님

        // When
        boolean result = handler.canHandle(accessor);

        // Then
        org.assertj.core.api.Assertions.assertThat(result).isFalse();
    }

    @Test
    @DisplayName("커맨드와 목적지가 모두 일치하면 canHandle은 true를 반환한다")
    void canHandle_True_WhenMatch() {
        // Given
        org.springframework.messaging.simp.stomp.StompHeaderAccessor accessor = 
            org.springframework.messaging.simp.stomp.StompHeaderAccessor.create(org.springframework.messaging.simp.stomp.StompCommand.UNSUBSCRIBE);
        accessor.setDestination("/sub/contents/123/chat");

        // When
        boolean result = handler.canHandle(accessor);

        // Then
        org.assertj.core.api.Assertions.assertThat(result).isTrue();
    }
}
