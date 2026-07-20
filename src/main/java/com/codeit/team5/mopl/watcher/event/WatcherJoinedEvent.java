package com.codeit.team5.mopl.watcher.event;

import com.codeit.team5.mopl.global.outbox.event.RetryableOutboxEvent;
import java.util.UUID;

public record WatcherJoinedEvent(UUID contentId, UUID watcherId) implements RetryableOutboxEvent {

}
