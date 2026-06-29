package com.codeit.team5.mopl.global.web.ws.stomp.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.global.web.ws.stomp.handler.StompCommandHandler;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

class StompInterceptorTest {

    @Test
    @DisplayName("커맨드가 없는 메세지는 그대로 통과한다_성공")
    void preSend_NoCommand() {
        // Given
        StompInterceptor interceptor = new StompInterceptor(List.of());
        Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();
        MessageChannel channel = mock(MessageChannel.class);

        // When
        Message<?> result = interceptor.preSend(message, channel);

        // Then
        assertThat(result).isEqualTo(message);
    }

    @Test
    @DisplayName("핸들러 중 하나라도 canHandle이면 handle을 호출하고 루프를 탈출한다_성공")
    void preSend_HandleAndBreak() {
        // Given
        StompCommandHandler handler1 = mock(StompCommandHandler.class);
        StompCommandHandler handler2 = mock(StompCommandHandler.class);
        
        StompInterceptor interceptor = new StompInterceptor(List.of(handler1, handler2));
        
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        MessageChannel channel = mock(MessageChannel.class);

        given(handler1.canHandle(any(StompHeaderAccessor.class))).willReturn(true);

        // When
        Message<?> result = interceptor.preSend(message, channel);

        // Then
        assertThat(result).isEqualTo(message);
        verify(handler1).handle(any(StompHeaderAccessor.class));
        verify(handler2, never()).canHandle(any(StompHeaderAccessor.class));
        verify(handler2, never()).handle(any(StompHeaderAccessor.class));
    }
}
