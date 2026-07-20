package com.codeit.team5.mopl.content.event;

import com.codeit.team5.mopl.global.outbox.event.RetryableOutboxEvent;
import java.util.List;
import java.util.UUID;

public record ContentUpsertedEvent(List<UUID> contentIds) implements RetryableOutboxEvent {

}
