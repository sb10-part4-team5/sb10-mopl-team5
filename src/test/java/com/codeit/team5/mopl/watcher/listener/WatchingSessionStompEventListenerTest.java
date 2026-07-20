package com.codeit.team5.mopl.watcher.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore.StompDestination;
import com.codeit.team5.mopl.watcher.constant.WatcherStatus;
import com.codeit.team5.mopl.watcher.dto.payload.WatchingSessionPayload;
import com.codeit.team5.mopl.watcher.service.WatchingSessionCommandService;
import com.codeit.team5.mopl.watcher.service.WatchingSessionQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@ExtendWith(MockitoExtension.class)
class WatchingSessionStompEventListenerTest {

    @Mock
    private WatchingSessionQueryService queryService;

    @Mock
    private WatchingSessionCommandService commandService;

    @Mock
    private WebSocketSessionStore sessionStore;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WatchingSessionStompEventListener listener;

    @Test
    @DisplayName("SessionDisconnectEvent 처리 - 정상 흐름")
    void handle_SessionDisconnectEvent_NormalFlow() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        Principal mockPrincipal = mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn(userId.toString());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setUser(mockPrincipal);

        StompDestination dest1 = mock(StompDestination.class);
        when(dest1.getPattern()).thenReturn("different-pattern");
        StompDestination dest2 = mock(StompDestination.class);
        when(dest2.getPattern()).thenReturn(StompConstants.SUB_WATCHING_CONTENT.replace("{id}", "*"));
        when(dest2.targetId()).thenReturn(UUID.randomUUID());

        List<StompDestination> destinations = List.of(dest1, dest2);
        when(sessionStore.getAllDestination(userId)).thenReturn(destinations);

        WatchingSessionPayload payload = mock(WatchingSessionPayload.class);
        when(queryService.getWatchingSessionPayload(userId, WatcherStatus.LEAVE))
                .thenReturn(payload);
        when(objectMapper.writeValueAsString(any())).thenReturn("dummy_json");

        Message<byte[]> message =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        SessionDisconnectEvent event =
                new SessionDisconnectEvent(this, message, "session-id", null);

        // when
        listener.handle(event);

        // then
        verify(sessionStore).getAllDestination(userId);
        verify(queryService).getWatchingSessionPayload(userId, WatcherStatus.LEAVE);
        verify(commandService).left(any(UUID.class), eq(userId));
        verify(redisTemplate).convertAndSend(eq("watching-session-topic"), eq("dummy_json"));
        verify(sessionStore).disconnect(userId);
    }

    @Test
    @DisplayName("SessionDisconnectEvent 처리 - 일치하는 패턴 없음")
    void handle_SessionDisconnectEvent_NoMatchingPattern() {
        // given
        UUID userId = UUID.randomUUID();
        Principal mockPrincipal = mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn(userId.toString());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setUser(mockPrincipal);

        StompDestination dest1 = mock(StompDestination.class);
        when(dest1.getPattern()).thenReturn("different-pattern1");
        StompDestination dest2 = mock(StompDestination.class);
        when(dest2.getPattern()).thenReturn("different-pattern2");

        List<StompDestination> destinations = List.of(dest1, dest2);
        when(sessionStore.getAllDestination(userId)).thenReturn(destinations);

        Message<byte[]> message =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        SessionDisconnectEvent event =
                new SessionDisconnectEvent(this, message, "session-id", null);

        // when
        listener.handle(event);

        // then
        verify(sessionStore).getAllDestination(userId);
        verifyNoInteractions(queryService);
        verifyNoInteractions(commandService);
        verifyNoInteractions(redisTemplate);
        verify(sessionStore).disconnect(userId);
    }

    @Test
    @DisplayName("SessionDisconnectEvent 처리 - accessor 또는 user null")
    void handle_SessionDisconnectEvent_NullAccessorOrUser() {
        // given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);

        Message<byte[]> message =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        SessionDisconnectEvent event =
                new SessionDisconnectEvent(this, message, "session-id", null);

        // when
        listener.handle(event);

        // then
        verifyNoInteractions(sessionStore);
        verifyNoInteractions(queryService);
        verifyNoInteractions(commandService);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("SessionUnsubscribeEvent 처리 - 정상 흐름")
    void handle_SessionUnsubscribeEvent_NormalFlow() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        String subId = "sub-123";
        Principal mockPrincipal = mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn(userId.toString());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
        accessor.setUser(mockPrincipal);
        accessor.setSubscriptionId(subId);

        String key = "%s/%s".formatted(userId, subId);
        WatchingSessionPayload payload = mock(WatchingSessionPayload.class);
        com.codeit.team5.mopl.watcher.dto.response.WatchingSessionResponse response =
                mock(com.codeit.team5.mopl.watcher.dto.response.WatchingSessionResponse.class);
        com.codeit.team5.mopl.content.dto.response.ContentResponse contentResponse =
                mock(com.codeit.team5.mopl.content.dto.response.ContentResponse.class);
        when(payload.response()).thenReturn(response);
        when(response.content()).thenReturn(contentResponse);
        when(contentResponse.id()).thenReturn(UUID.randomUUID());
        Map<String, Object> sessionAttributes = new java.util.HashMap<>();
        sessionAttributes.put(key, payload);
        accessor.setSessionAttributes(sessionAttributes);
        
        when(objectMapper.writeValueAsString(any())).thenReturn("dummy_json");

        Message<byte[]> message =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        SessionUnsubscribeEvent event = new SessionUnsubscribeEvent(this, message, mockPrincipal);

        // when
        listener.handle(event);

        // then
        verify(redisTemplate).convertAndSend(eq("watching-session-topic"), eq("dummy_json"));
        assertThat(accessor.getSessionAttributes()).doesNotContainKey(key);
    }

    @Test
    @DisplayName("SessionUnsubscribeEvent 처리 - sessionAttributes 또는 payload null")
    void handle_SessionUnsubscribeEvent_NullSessionAttributesOrPayload() {
        // given
        UUID userId = UUID.randomUUID();
        String subId = "sub-123";
        Principal mockPrincipal = mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn(userId.toString());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
        accessor.setUser(mockPrincipal);
        accessor.setSubscriptionId(subId);
        Map<String, Object> sessionAttributes = new java.util.HashMap<>();
        accessor.setSessionAttributes(sessionAttributes);

        Message<byte[]> message =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        SessionUnsubscribeEvent event = new SessionUnsubscribeEvent(this, message, mockPrincipal);

        // when
        listener.handle(event);

        // then
        verifyNoInteractions(redisTemplate);
    }
}
