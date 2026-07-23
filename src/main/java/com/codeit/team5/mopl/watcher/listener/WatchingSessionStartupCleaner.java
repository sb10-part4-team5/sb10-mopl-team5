package com.codeit.team5.mopl.watcher.listener;

import com.codeit.team5.mopl.content.repository.ContentStatsRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchingSessionStartupCleaner {

    private final ContentStatsRepository statsRepository;
    private final StringRedisTemplate redisTemplate;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onApplicationReady() {
        log.info("Application started. Initializing all watcher counts to 0 and clearing Redis sessions...");
        
        // 1. DB의 시청자 수 0으로 초기화
        statsRepository.resetAllWatcherCounts();
        
        // 2. Redis의 시청 세션 초기화
        Set<String> watcherKeys = redisTemplate.keys("watcher:session:*");
        if (watcherKeys != null && !watcherKeys.isEmpty()) {
            redisTemplate.delete(watcherKeys);
        }
        
        Set<String> contentKeys = redisTemplate.keys("content:*:watchers");
        if (contentKeys != null && !contentKeys.isEmpty()) {
            redisTemplate.delete(contentKeys);
        }
        
        log.info("Finished initializing watcher counts and Redis sessions.");
    }
}
