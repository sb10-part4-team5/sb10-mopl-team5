package com.codeit.team5.mopl.watcher.listener;

import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import com.codeit.team5.mopl.watcher.constant.WatcherStatus;
import com.codeit.team5.mopl.watcher.dto.payload.WatchingSessionPayload;
import com.codeit.team5.mopl.watcher.dto.payload.WatchingSessionRedisMessage;
import com.codeit.team5.mopl.watcher.event.WatcherJoinedEvent;
import com.codeit.team5.mopl.watcher.service.WatchingSessionQueryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.codeit.team5.mopl.watcher.constant.WatcherRedisConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchingSessionEventRedisPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final WatchingSessionQueryService queryService;

    @EventListener
    public void onWatcherJoined(WatcherJoinedEvent event) {
        try {
            WatchingSessionPayload payload =
                    queryService.getWatchingSessionPayload(event.contentId(), event.watcherId(), WatcherStatus.JOIN);
            publishEvent(new WatchingSessionRedisMessage(event.contentId(), payload));
        } catch (Exception e) {
            log.error("Failed to process WatcherJoinedEvent for Redis publish", e);
        }
    }

    private void publishEvent(Object event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            stringRedisTemplate.convertAndSend(WatcherRedisConstants.WATCHING_SESSION_TOPIC, message);
            log.info("Published to Redis topic '{}': {}", WatcherRedisConstants.WATCHING_SESSION_TOPIC, message);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event for Redis publish", e);
        }
    }
}
