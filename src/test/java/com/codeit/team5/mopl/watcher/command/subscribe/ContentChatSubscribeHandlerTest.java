package com.codeit.team5.mopl.watcher.command.subscribe;

import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import com.codeit.team5.mopl.watcher.service.WatchingSessionService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentChatSubscribeHandlerTest {

    @Mock
    private WebSocketSessionStore sessionStore;

    @Mock
    private WatchingSessionService service;

    @InjectMocks
    private ContentChatSubscribeHandler handler;

    @Test
    @DisplayName("doHandle 호출 시 service의 ensureWatchingContent를 호출한다_성공")
    void doHandle_Success() {
        // Given
        UUID contentId = UUID.randomUUID();
        String email = "test@test.com";

        // When
        handler.doHandle(contentId, email);

        // Then
        verify(service).ensureWatchingContent(email, contentId);
    }
}
