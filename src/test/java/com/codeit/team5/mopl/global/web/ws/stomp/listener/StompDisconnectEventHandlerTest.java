package com.codeit.team5.mopl.global.web.ws.stomp.listener;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import com.codeit.team5.mopl.watcher.exception.WatchingSessionNotFoundException;
import com.codeit.team5.mopl.watcher.service.WatchingSessionService;
import java.security.Principal;
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

@ExtendWith(MockitoExtension.class)
class StompDisconnectEventHandlerTest {

    @Mock
    private WebSocketSessionStore sessionStore;

    @Mock
    private WatchingSessionService watchingSessionService;

    @InjectMocks
    private StompDisconnectEventHandler listener;

    @Test
    @DisplayName("WebSocket 연결이 끊어지면 SessionStore와 DB에서 세션을 삭제한다_성공")
    void handleWebSocketDisconnectListener_Success() {
        // given
        String email = "test@test.com";
        Principal mockPrincipal = mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn(email);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setUser(mockPrincipal);

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0],
                accessor.getMessageHeaders());
        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, "session-id",
                null);

        // when
        listener.handleWebSocketDisconnectListener(event);

        // then
        verify(sessionStore).disconnect(email);
        verify(watchingSessionService).delete(email);
    }

    @Test
    @DisplayName("유저 정보가 없으면 조기 종료된다_성공")
    void handleWebSocketDisconnectListener_NoUser() {
        // given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0],
                accessor.getMessageHeaders());
        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, "session-id",
                null);

        // when
        listener.handleWebSocketDisconnectListener(event);

        // then
        verifyNoInteractions(sessionStore);
        verifyNoInteractions(watchingSessionService);
    }

    @Test
    @DisplayName("watchingSessionService.delete에서 예외가 발생해도 전파되지 않고 완료된다_성공")
    void handleWebSocketDisconnectListener_IgnoreDeleteException() {
        // given
        String email = "test@test.com";
        Principal mockPrincipal = mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn(email);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setUser(mockPrincipal);

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0],
                accessor.getMessageHeaders());
        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, "session-id",
                null);

        doThrow(new WatchingSessionNotFoundException("email", email)).when(watchingSessionService)
                .delete(email);

        // when
        listener.handleWebSocketDisconnectListener(event);

        // then
        verify(sessionStore).disconnect(email);
        verify(watchingSessionService).delete(email);
    }
}
