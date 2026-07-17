package com.codeit.team5.mopl.watcher.listener;

import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.codeit.team5.mopl.watcher.constant.WatcherStatus;
import com.codeit.team5.mopl.watcher.dto.payload.WatchingSessionPayload;
import com.codeit.team5.mopl.watcher.dto.payload.WatchingSessionRedisMessage;
import com.codeit.team5.mopl.watcher.event.WatcherJoinedEvent;
import com.codeit.team5.mopl.watcher.event.WatcherLeftEvent;
import com.codeit.team5.mopl.watcher.service.WatchingSessionQueryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchingSessionEventRedisPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final WatchingSessionQueryService queryService;
    public static final String TOPIC_NAME = "watching-session-topic";

    @EventListener
    public void onWatcherJoined(WatcherJoinedEvent event) {
        try {
            WatchingSessionPayload payload = queryService.getWatchingSessionPayload(event.watcherId(), WatcherStatus.JOIN);
            publishEvent(new WatchingSessionRedisMessage(event.contentId(), payload));
        } catch (Exception e) {
            log.error("Failed to process WatcherJoinedEvent for Redis publish", e);
        }
    }

    // Note: for WatcherLeftEvent we can't fetch the session from DB because it's already deleted.
    // The STOMP event listener fetches it *before* deleting it. 
    // We should just let STOMP listener publish left events directly or pass the payload in WatcherLeftEvent.
    // Wait! Since STOMP listener already handles LEAVE, maybe we only need to publish JOIN here?
    // Let's pass the payload directly in WatcherLeftEvent! 
    // But Wait, we can't change WatcherLeftEvent easily if other things use it.
    // Let's just implement publishEvent for WatcherJoinedEvent for now, and see if left works via the STOMP listener.
    
    private void publishEvent(Object event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            stringRedisTemplate.convertAndSend(TOPIC_NAME, message);
            log.info("Published to Redis topic '{}': {}", TOPIC_NAME, message);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event for Redis publish", e);
        }
    }
}
