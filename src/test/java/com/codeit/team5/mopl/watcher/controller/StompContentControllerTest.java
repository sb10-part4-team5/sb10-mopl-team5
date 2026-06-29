package com.codeit.team5.mopl.watcher.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.watcher.dto.payload.ContentChatPayload;
import com.codeit.team5.mopl.watcher.dto.request.ContentChatCreatedRequest;
import com.codeit.team5.mopl.watcher.dto.response.WatcherResponse;
import com.codeit.team5.mopl.watcher.service.ContentChatService;
import java.security.Principal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class StompContentControllerTest {

    @Mock
    private ContentChatService chatService;

    @InjectMocks
    private StompContentController controller;

    @Test
    @DisplayName("채팅 메세지를 받으면 Service를 호출하고 Payload를 반환한다_성공")
    void sendChat_Success() {
        // Given
        String email = "test@test.com";
        Principal mockPrincipal = mock(Principal.class);
        given(mockPrincipal.getName()).willReturn(email);

        ContentChatCreatedRequest request = new ContentChatCreatedRequest("Hello MOPL!");
        WatcherResponse mockWatcher = new WatcherResponse(UUID.randomUUID(), email, null);
        ContentChatPayload expectedPayload = new ContentChatPayload(mockWatcher, "Hello MOPL!");

        given(chatService.createContentChatPayload(eq(email), any(ContentChatCreatedRequest.class)))
                .willReturn(expectedPayload);

        // When
        ContentChatPayload result = controller.sendChat(mockPrincipal, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.content()).isEqualTo("Hello MOPL!");
        verify(chatService).createContentChatPayload(email, request);
    }
}
