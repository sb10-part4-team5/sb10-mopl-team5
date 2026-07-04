package com.codeit.team5.mopl.watcher.command.unsubscribe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
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
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

@ExtendWith(MockitoExtension.class)
class WatchingContentUnsubscribeHandlerTest {

    @Mock
    private WebSocketSessionStore sessionStore;

    @Mock
    private WatchingSessionService service;

    @Mock
    private WatchingSessionPayloadSender payloadSender;

    @InjectMocks
    private WatchingContentUnsubscribeHandler handler;

    @Test
    @DisplayName("doHandle ?몄텧 ???몄뀡??寃利? ??젣?섍퀬 ?댁옣 硫붿떆吏瑜?釉뚮줈?쒖틦?ㅽ듃?쒕떎_?깃났")
    void doHandle_Success() {
        // Given
        UUID contentId = UUID.randomUUID();
        UUID email = UUID.randomUUID();
        WatchingSessionResponse response = WatchingSessionResponse.builder().build();
        long watchCount = 4L;

        given(service.findSessionByWatchId(email)).willReturn(response);
        given(service.getCurrentWatchingContentView(contentId)).willReturn(watchCount);

        // When
        handler.doHandle(contentId, email);

        // Then
        InOrder inOrder = inOrder(service, payloadSender);
        inOrder.verify(service).ensureWatchingContent(email, contentId);
        inOrder.verify(service).findSessionByWatchId(email);
        inOrder.verify(service).delete(email);
        inOrder.verify(service).getCurrentWatchingContentView(contentId);
        inOrder.verify(payloadSender).send(contentId, new WatchingSessionPayload(WatcherStatus.LEAVE, response, watchCount));
    }

    @Test
    @DisplayName("而ㅻ㎤?쒓? ?ㅻⅤ硫?canHandle? false瑜?諛섑솚?쒕떎")
    void canHandle_False_WhenCommandIsDifferent() {
        // Given
        StompHeaderAccessor accessor =
            StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/sub/contents/123/watch");

        // When
        boolean result = handler.canHandle(accessor);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("紐⑹쟻吏媛 ?ㅻⅤ硫?canHandle? false瑜?諛섑솚?쒕떎")
    void canHandle_False_WhenDestinationIsDifferent() {
        // Given
        StompHeaderAccessor accessor =
            StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
        accessor.setDestination("/sub/contents/123/chat"); // watch媛 ?꾨떂

        // When
        boolean result = handler.canHandle(accessor);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("而ㅻ㎤?쒖? 紐⑹쟻吏媛 紐⑤몢 ?쇱튂?섎㈃ canHandle? true瑜?諛섑솚?쒕떎")
    void canHandle_True_WhenMatch() {
        // Given
        StompHeaderAccessor accessor =
            StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
        accessor.setDestination("/sub/contents/123/watch");

        // When
        boolean result = handler.canHandle(accessor);

        // Then
        assertThat(result).isTrue();
    }
}

