package com.codeit.team5.mopl.dm.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.messaging.simp.stomp.StompCommand.SUBSCRIBE;
import static org.springframework.messaging.simp.stomp.StompCommand.UNSUBSCRIBE;

import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

@ExtendWith(MockitoExtension.class)
class DmUnsubscribeHandlerTest {

    @Mock
    private WebSocketSessionStore sessionStore;

    @InjectMocks
    private DmUnsubscribeHandler handler;

    @Test
    @DisplayName("doHandle 호출 시 아무 동작도 하지 않는다_성공")
    void doHandle_Success() {
        // Given
        UUID conversationId = UUID.randomUUID();
        String email = "test@test.com";

        // When
        handler.doHandle(conversationId, email);

        // Then
        verifyNoInteractions(sessionStore);
    }

    @Test
    @DisplayName("커맨드가 다르면 canHandle은 false를 반환한다_실패")
    void canHandle_Fail_WhenCommandIsDifferent() {
        // Given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(SUBSCRIBE);
        accessor.setDestination("/sub/conversations/" + UUID.randomUUID() + "/direct-messages");

        // When
        boolean result = handler.canHandle(accessor);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("목적지가 다르면 canHandle은 false를 반환한다_실패")
    void canHandle_Fail_WhenDestinationIsDifferent() {
        // Given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(UNSUBSCRIBE);
        accessor.setDestination("/sub/conversations/" + UUID.randomUUID() + "/other");

        // When
        boolean result = handler.canHandle(accessor);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("커맨드와 목적지가 모두 일치하면 canHandle은 true를 반환한다_성공")
    void canHandle_Success_WhenMatch() {
        // Given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(UNSUBSCRIBE);
        accessor.setDestination("/sub/conversations/" + UUID.randomUUID() + "/direct-messages");

        // When
        boolean result = handler.canHandle(accessor);

        // Then
        assertThat(result).isTrue();
    }
}
