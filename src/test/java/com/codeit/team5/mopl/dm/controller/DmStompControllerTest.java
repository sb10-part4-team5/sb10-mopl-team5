package com.codeit.team5.mopl.dm.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.dm.dto.request.DirectMessageSendRequest;
import com.codeit.team5.mopl.dm.service.DmService;
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
    private DmService dmService;

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
        verify(dmService).sendMessage("a@mopl.com", conversationId, "hello");
    }
}
