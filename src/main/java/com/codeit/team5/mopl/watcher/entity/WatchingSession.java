package com.codeit.team5.mopl.watcher.entity;

import java.time.Instant;
import java.util.UUID;

public record WatchingSession(UUID watcherId, UUID contentId, Instant createdAt) {
}
