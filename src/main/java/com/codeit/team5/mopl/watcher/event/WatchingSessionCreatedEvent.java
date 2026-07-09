package com.codeit.team5.mopl.watcher.event;

import java.util.UUID;

public record WatchingSessionCreatedEvent(
    UUID watchingSessionId
) {
}
