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
}
