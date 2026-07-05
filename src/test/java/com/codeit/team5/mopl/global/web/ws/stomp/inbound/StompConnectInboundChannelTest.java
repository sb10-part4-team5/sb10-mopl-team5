package com.codeit.team5.mopl.global.web.ws.stomp.inbound;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import com.codeit.team5.mopl.global.web.ws.stomp.base.BaseStompInboundChannelTest;

class StompConnectInboundChannelTest extends BaseStompInboundChannelTest {

    @Test
    @DisplayName("STOMP CONNECT 메시지가 인입되면 세션이 연결된다")
    void inboundConnectTest() {
        // given
        UUID testUserId = UUID.randomUUID();
        String validToken = "valid-token";

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(testUserId.toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));

        when(jwtAuthenticationService.getAuthentication(validToken)).thenReturn(authentication);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + validToken);

        Message<byte[]> message =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // when
        clientInboundChannel.send(message);

        // then
        verify(sessionStore).connect(testUserId);
    }

    @Test
    @DisplayName("STOMP CONNECT 메시지에 사용자 정보가 없으면 예외가 발생한다_실패")
    void inboundConnectTest_Fail_NoUser() {
        // given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        // Authorization 헤더 세팅 안함

        Message<byte[]> message =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // when & then
        assertThatThrownBy(() -> clientInboundChannel.send(message)).cause()
                .isInstanceOf(io.jsonwebtoken.JwtException.class).hasMessage("token not found");
    }
}
