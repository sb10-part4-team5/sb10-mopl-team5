package com.codeit.team5.mopl.global.web.ws.stomp.listener;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import com.codeit.team5.mopl.watcher.exception.WatchingSessionNotFoundException;
import com.codeit.team5.mopl.watcher.service.WatchingSessionCommandService;

@ExtendWith(MockitoExtension.class)
class StompDisconnectEventHandlerTest {

    @Mock
    private WebSocketSessionStore sessionStore;

    @Mock
    private WatchingSessionCommandService watchingSessionService;

    @InjectMocks
    private StompDisconnectEventHandler listener;

    @Test
    @DisplayName("WebSocket 연결이 끊어지면 SessionStore와 DB에서 정보를 삭제한다_성공")
    void handleWebSocketDisconnectListener_Success() {
        // given
        UUID userId = UUID.randomUUID();
        Principal mockPrincipal = mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn(userId.toString());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setUser(mockPrincipal);

        Message<byte[]> message =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        SessionDisconnectEvent event =
                new SessionDisconnectEvent(this, message, "session-id", null);

        // when
        listener.handleWebSocketDisconnectListener(event);

        // then
        verify(sessionStore).disconnect(userId);
        verify(watchingSessionService).left(userId);
    }

    @Test
    @DisplayName("저장된 세션이 없으면 예외가 발생하지 않는다_성공")
    void handleWebSocketDisconnectListener_NoUser() {
        // given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        Message<byte[]> message =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        SessionDisconnectEvent event =
                new SessionDisconnectEvent(this, message, "session-id", null);

        // when
        listener.handleWebSocketDisconnectListener(event);

        // then
        verifyNoInteractions(sessionStore);
        verifyNoInteractions(watchingSessionService);
    }

    @Test
    @DisplayName("watchingSessionService.left에서 예외가 발생해도 정상 처리된다_성공")
    void handleWebSocketDisconnectListener_IgnoreDeleteException() {
        // given
        UUID userId = UUID.randomUUID();
        Principal mockPrincipal = mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn(userId.toString());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setUser(mockPrincipal);

        Message<byte[]> message =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        SessionDisconnectEvent event =
                new SessionDisconnectEvent(this, message, "session-id", null);

        doThrow(new WatchingSessionNotFoundException(Map.of("userId", userId)))
                .when(watchingSessionService).left(userId);

        // when
        listener.handleWebSocketDisconnectListener(event);

        // then
        verify(sessionStore).disconnect(userId);
        verify(watchingSessionService).left(userId);
    }
}
