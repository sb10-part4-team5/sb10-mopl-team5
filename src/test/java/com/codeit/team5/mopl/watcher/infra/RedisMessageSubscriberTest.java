package com.codeit.team5.mopl.watcher.infra;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.watcher.dto.payload.WatchingSessionPayload;
import com.codeit.team5.mopl.watcher.dto.payload.WatchingSessionRedisMessage;
import com.codeit.team5.mopl.watcher.provider.WatchingSessionPayloadSender;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;

@ExtendWith(MockitoExtension.class)
class RedisMessageSubscriberTest {

    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private WatchingSessionPayloadSender payloadSender;

    @InjectMocks
    private RedisMessageSubscriber subscriber;

    @Test
    @DisplayName("onMessage - 성공적으로 페이로드 전송")
    void onMessage_Success() throws Exception {
        // given
        Message message = mock(Message.class);
        byte[] pattern = "pattern".getBytes(StandardCharsets.UTF_8);
        String bodyString = "{\"contentId\":\"123e4567-e89b-12d3-a456-426614174000\"}";
        byte[] body = bodyString.getBytes(StandardCharsets.UTF_8);
        
        when(message.getBody()).thenReturn(body);
        
        UUID contentId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        WatchingSessionPayload payload = mock(WatchingSessionPayload.class);
        WatchingSessionRedisMessage redisMessage = new WatchingSessionRedisMessage(contentId, payload);
        
        when(objectMapper.readValue(bodyString, WatchingSessionRedisMessage.class)).thenReturn(redisMessage);

        // when
        subscriber.onMessage(message, pattern);

        // then
        verify(payloadSender).send(eq(contentId), eq(payload));
    }

    @Test
    @DisplayName("onMessage - 역직렬화 실패시 예외 처리 및 로깅")
    void onMessage_Exception() throws Exception {
        // given
        Message message = mock(Message.class);
        byte[] pattern = "pattern".getBytes(StandardCharsets.UTF_8);
        String bodyString = "invalid json";
        byte[] body = bodyString.getBytes(StandardCharsets.UTF_8);
        
        when(message.getBody()).thenReturn(body);
        when(objectMapper.readValue(bodyString, WatchingSessionRedisMessage.class)).thenThrow(new RuntimeException("parse error"));

        // when
        subscriber.onMessage(message, pattern);

        // then
        verifyNoInteractions(payloadSender);
    }
}
