package com.codeit.team5.mopl.watcher.listener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.codeit.team5.mopl.content.repository.ContentStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
        statsRepository.resetAllWatcherCounts(Instant.now());

        // 2. Redis의 시청 세션 초기화
        deleteKeysByPattern("watcher:session:*");
        deleteKeysByPattern("content:*:watchers");

        log.info("Finished initializing watcher counts and Redis sessions.");
    }

    private void deleteKeysByPattern(String pattern) {
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
        try {
            redisTemplate.execute((RedisConnection connection) -> deleteScannedKeys(connection, options));
        } catch (Exception e) {
            log.error("Redis scan error during startup cleanup for pattern: {}", pattern, e);
        }
    }

    private Object deleteScannedKeys(RedisConnection connection, ScanOptions options) {
        try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
            processCursor(connection, cursor);
        }
        return null;
    }

    private void processCursor(RedisConnection connection, Cursor<byte[]> cursor) {
        List<byte[]> keys = new ArrayList<>();
        while (cursor.hasNext()) {
            keys.add(cursor.next());
            flushIfFull(connection, keys);
        }
        flushKeys(connection, keys);
    }

    private void flushIfFull(RedisConnection connection, List<byte[]> keys) {
        if (keys.size() >= 100) {
            flushKeys(connection, keys);
        }
    }

    private void flushKeys(RedisConnection connection, List<byte[]> keys) {
        if (keys.isEmpty()) {
            return;
        }
        connection.keyCommands().del(keys.toArray(new byte[0][]));
        keys.clear();
    }
}
