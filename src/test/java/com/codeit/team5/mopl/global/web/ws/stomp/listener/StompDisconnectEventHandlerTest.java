package com.codeit.team5.mopl.global.web.ws.stomp.listener;

import java.util.UUID;

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
    @DisplayName("WebSocket ?怨뚭퍙????녿선筌왖筌?SessionStore?? DB?癒?퐣 ?紐꾨???????뺣뼄_?源껊궗")
    void handleWebSocketDisconnectListener_Success() {
        // given
        UUID userId = UUID.randomUUID();
        Principal mockPrincipal = mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn(userId.toString());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setUser(mockPrincipal);

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0],
                accessor.getMessageHeaders());
        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, "session-id",
                null);

        // when
        listener.handleWebSocketDisconnectListener(event);

        // then
        verify(sessionStore).disconnect(userId);
        verify(watchingSessionService).delete(userId);
    }

    @Test
    @DisplayName("?醫? ?類ｋ궖揶쎛 ??곸몵筌?鈺곌퀗由??ル굝利??뺣뼄_?源껊궗")
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
    @DisplayName("watchingSessionService.delete?癒?퐣 ??됱뇚揶쎛 獄쏆뮇源??猷??袁る솁??? ??꾪??袁⑥┷??뺣뼄_?源껊궗")
    void handleWebSocketDisconnectListener_IgnoreDeleteException() {
        // given
        UUID userId = UUID.randomUUID();
        Principal mockPrincipal = mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn(userId.toString());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setUser(mockPrincipal);

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0],
                accessor.getMessageHeaders());
        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, "session-id",
                null);

        doThrow(new WatchingSessionNotFoundException(java.util.Map.of("userId", userId))).when(watchingSessionService)
                .delete(userId);

        // when
        listener.handleWebSocketDisconnectListener(event);

        // then
        verify(sessionStore).disconnect(userId);
        verify(watchingSessionService).delete(userId);
    }
}


