package com.codeit.team5.mopl.dm.controller;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.dm.dto.request.DirectMessageSendRequest;
import com.codeit.team5.mopl.dm.service.DirectMessageService;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import java.security.Principal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DmStompControllerTest {

    @Mock
    private DirectMessageService directMessageService;

    @Mock
    private Principal principal;

    @InjectMocks
    private DmStompController dmStompController;

    @Test
    @DisplayName("DM 메시지 송신 위임 성공")
    void sendDirectMessage_delegatesToService_success() {
        // given
        UUID conversationId = UUID.randomUUID();
        DirectMessageSendRequest request = new DirectMessageSendRequest("hello");
        when(principal.getName()).thenReturn("a@mopl.com");

        // when
        dmStompController.sendDirectMessage(principal, conversationId, request);

        // then
        verify(directMessageService).sendMessage("a@mopl.com", conversationId, "hello");
    }

    @Test
    @DisplayName("유효성 검증 실패 시 principal null이어도 예외 없이 처리 성공")
    void handleValidationException_nullPrincipal_doesNotThrow_success() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);

        assertThatNoException().isThrownBy(() ->
                dmStompController.handleValidationException(ex, null));
    }

    @Test
    @DisplayName("유효성 검증 실패 시 principal 있으면 경고 로그 후 예외 없이 처리 성공")
    void handleValidationException_withPrincipal_doesNotThrow_success() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(principal.getName()).thenReturn("a@mopl.com");

        assertThatNoException().isThrownBy(() ->
                dmStompController.handleValidationException(ex, principal));
    }
}
