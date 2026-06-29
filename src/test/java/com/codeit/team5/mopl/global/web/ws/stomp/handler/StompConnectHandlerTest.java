package com.codeit.team5.mopl.global.web.ws.stomp.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.auth.jwt.JwtTokenizer;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class StompConnectHandlerTest {

    @Mock
    private JwtTokenizer jwtTokenizer;

    @Mock
    private WebSocketSessionStore sessionStore;

    @InjectMocks
    private StompConnectHandler handler;

    @Test
    @DisplayName("CONNECT 커맨드일 때 canHandle은 true를 반환한다_성공")
    void canHandle_Connect_True() {
        // given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);

        // when
        boolean result = handler.canHandle(accessor);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("CONNECT 커맨드가 아닐 때 canHandle은 false를 반환한다_성공")
    void canHandle_NotConnect_False() {
        // given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);

        // when
        boolean result = handler.canHandle(accessor);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("유효한 토큰이 주어지면 Principal이 설정되고 connect된다_성공")
    void handle_ValidToken_Success() {
        // given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer valid-token");

        Authentication auth = new UsernamePasswordAuthenticationToken("test@test.com", "", Collections.emptyList());
        when(jwtTokenizer.getAuthentication("valid-token")).thenReturn(auth);

        // when
        handler.handle(accessor);

        // then
        assertThat(accessor.getUser()).isEqualTo(auth);
        verify(sessionStore).connect("test@test.com");
    }

    @Test
    @DisplayName("토큰이 없으면 예외가 발생한다_실패")
    void handle_NoToken_ThrowsException() {
        // given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);

        // when & then
        assertThatThrownBy(() -> handler.handle(accessor))
                .isInstanceOf(JwtException.class)
                .hasMessage("token not found");
    }
}
