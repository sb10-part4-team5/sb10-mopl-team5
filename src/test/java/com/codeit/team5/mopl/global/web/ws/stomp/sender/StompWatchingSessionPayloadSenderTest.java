package com.codeit.team5.mopl.global.web.ws.stomp.sender;

import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class StompWatchingSessionPayloadSenderTest {

    @Mock
    private SimpMessagingTemplate template;

    @InjectMocks
    private StompWatchingSessionPayloadSender sender;

    @Test
    @DisplayName("payload를 targetId 목적지로 변환하여 전송한다_성공")
    void send_Success() {
        // Given
        UUID targetId = UUID.randomUUID();
        Object payload = new Object();
        String expectedDestination = StompConstants.SUB_WATCHING_CONTENT.replace("{id}", targetId.toString());

        // When
        sender.send(targetId, payload);

        // Then
        verify(template).convertAndSend(expectedDestination, payload);
    }
}
