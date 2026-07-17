package com.codeit.team5.mopl.watcher.infra;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import com.codeit.team5.mopl.watcher.dto.payload.WatchingSessionRedisMessage;
import com.codeit.team5.mopl.watcher.provider.WatchingSessionPayloadSender;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMessageSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final WatchingSessionPayloadSender payloadSender;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            WatchingSessionRedisMessage redisMessage = objectMapper.readValue(body, WatchingSessionRedisMessage.class);
            payloadSender.send(redisMessage.contentId(), redisMessage.payload());
            log.debug("Received and sent watching session message for content: {}", redisMessage.contentId());
        } catch (Exception e) {
            log.error("Failed to process Redis message", e);
        }
    }
}
