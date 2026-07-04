package com.codeit.team5.mopl.watcher.command.unsubscribe;

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
class ContentChatUnsubscribeHandlerTest {

    @Mock
    private WebSocketSessionStore sessionStore;

    @InjectMocks
    private ContentChatUnsubscribeHandler handler;

    @Test
    @DisplayName("doHandle ?몄텧 ???꾨Т ?숈옉???섏? ?딅뒗???깃났")
    void doHandle_Success() {
        // Given
        UUID contentId = UUID.randomUUID();
        UUID email = UUID.randomUUID();

        // When
        handler.doHandle(contentId, email);

        // Then
        verifyNoInteractions(sessionStore);
    }

    @Test
    @DisplayName("而ㅻ㎤?쒓? ?ㅻⅤ硫?canHandle? false瑜?諛섑솚?쒕떎")
    void canHandle_False_WhenCommandIsDifferent() {
        // Given
        StompHeaderAccessor accessor =
            StompHeaderAccessor.create(SUBSCRIBE);
        accessor.setDestination("/sub/contents/123/chat");

        // When
        boolean result = handler.canHandle(accessor);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("紐⑹쟻吏媛 ?ㅻⅤ硫?canHandle? false瑜?諛섑솚?쒕떎")
    void canHandle_False_WhenDestinationIsDifferent() {
        // Given
        StompHeaderAccessor accessor =
            StompHeaderAccessor.create(UNSUBSCRIBE);
        accessor.setDestination("/sub/contents/123/watch"); // chat???꾨떂

        // When
        boolean result = handler.canHandle(accessor);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("而ㅻ㎤?쒖? 紐⑹쟻吏媛 紐⑤몢 ?쇱튂?섎㈃ canHandle? true瑜?諛섑솚?쒕떎")
    void canHandle_True_WhenMatch() {
        // Given
        StompHeaderAccessor accessor =
            StompHeaderAccessor.create(UNSUBSCRIBE);
        accessor.setDestination("/sub/contents/123/chat");

        // When
        boolean result = handler.canHandle(accessor);

        // Then
        assertThat(result).isTrue();
    }
}

