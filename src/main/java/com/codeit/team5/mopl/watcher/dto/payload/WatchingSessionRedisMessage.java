package com.codeit.team5.mopl.watcher.dto.payload;

import java.util.UUID;

public record WatchingSessionRedisMessage(UUID contentId, WatchingSessionPayload payload) {
}
