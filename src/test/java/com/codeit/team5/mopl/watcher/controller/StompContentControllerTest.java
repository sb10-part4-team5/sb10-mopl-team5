package com.codeit.team5.mopl.watcher.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.codeit.team5.mopl.auth.security.details.MoplPrincipal;
import com.codeit.team5.mopl.watcher.dto.payload.ContentChatPayload;
import com.codeit.team5.mopl.watcher.dto.request.ContentChatCreatedRequest;
import com.codeit.team5.mopl.user.dto.response.UserSummary;
import com.codeit.team5.mopl.watcher.service.ContentChatService;

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
        UUID watcherId = UUID.randomUUID();
        String name = "testName";
        MoplPrincipal mockMoplPrincipal = mock(MoplPrincipal.class);
        given(mockMoplPrincipal.getId()).willReturn(watcherId);
        ContentChatCreatedRequest request = new ContentChatCreatedRequest("Hello MOPL!");
        UserSummary mockWatcher = new UserSummary(watcherId, name, null);
        ContentChatPayload expectedPayload = new ContentChatPayload(mockWatcher, "Hello MOPL!");

        given(chatService.createContentChatPayload(eq(watcherId), any(ContentChatCreatedRequest.class)))
                .willReturn(expectedPayload);

        // When
        ContentChatPayload result = controller.sendChat(mockMoplPrincipal, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.content()).isEqualTo("Hello MOPL!");
        verify(chatService).createContentChatPayload(watcherId, request);
    }
}
