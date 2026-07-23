package com.codeit.team5.mopl.dm.infra;

import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.dm.provider.DirectMessageBroadcaster;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DmRedisMessageSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final DirectMessageBroadcaster directMessageBroadcaster;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            DirectMessageResponse response = objectMapper.readValue(body, DirectMessageResponse.class);
            directMessageBroadcaster.broadcast(response);
            log.debug("Received and broadcast DM message: conversationId={}", response.conversationId());
        } catch (Exception e) {
            log.error("Failed to process DM Redis message", e);
        }
    }
}
