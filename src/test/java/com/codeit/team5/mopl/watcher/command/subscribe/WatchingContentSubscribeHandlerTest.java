package com.codeit.team5.mopl.watcher.command.subscribe;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import com.codeit.team5.mopl.watcher.constant.WatcherStatus;
import com.codeit.team5.mopl.watcher.dto.payload.WatchingSessionPayload;
import com.codeit.team5.mopl.watcher.dto.response.WatchingSessionResponse;
import com.codeit.team5.mopl.watcher.provider.WatchingSessionPayloadSender;
import com.codeit.team5.mopl.watcher.service.WatchingSessionService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WatchingContentSubscribeHandlerTest {

    @Mock
    private WebSocketSessionStore sessionStore;

    @Mock
    private WatchingSessionService service;

    @Mock
    private WatchingSessionPayloadSender payloadSender;

    @InjectMocks
    private WatchingContentSubscribeHandler handler;

    @Test
    @DisplayName("doHandle 호출 시 세션을 생성하고 참여 메시지를 브로드캐스트한다_성공")
    void doHandle_Success() {
        // Given
        UUID contentId = UUID.randomUUID();
        String email = "test@test.com";
        WatchingSessionResponse response = WatchingSessionResponse.builder().build();
        long watchCount = 5L;

        given(service.create(contentId, email)).willReturn(response);
        given(service.getCurrentWatchingContentView(contentId)).willReturn(watchCount);

        // When
        handler.doHandle(contentId, email);

        // Then
        verify(service).create(contentId, email);
        verify(service).getCurrentWatchingContentView(contentId);
        verify(payloadSender).send(contentId, new WatchingSessionPayload(WatcherStatus.JOIN, response, watchCount));
    }
}
