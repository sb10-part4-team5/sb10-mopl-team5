package com.codeit.team5.mopl.global.web.ws.stomp.inbound;

import static org.mockito.Mockito.verify;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import com.codeit.team5.mopl.global.web.ws.stomp.base.BaseStompInboundChannelTest;

class StompDisconnectInboundChannelTest extends BaseStompInboundChannelTest {

    @Test
    @DisplayName("STOMP DISCONNECT 메시지가 인입되면 세션이 해제된다")
    void inboundDisconnectTest() {
        // given
        UUID testUserId = UUID.randomUUID();

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                testUserId.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        accessor.setUser(auth);
        accessor.setSessionId("test-session");

        Message<byte[]> message =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // when
        clientInboundChannel.send(message);

        // WebSocket DISCONNECT 이벤트는 Inbound 채널에서 직접 발생하지 않으므로 수동 발행하여 핸들러 연동 확인
        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message,
                accessor.getSessionId(), CloseStatus.NORMAL, auth);
        eventPublisher.publishEvent(event);

        // then
        verify(sessionStore).disconnect(testUserId);
    }
}
