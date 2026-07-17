package com.codeit.team5.mopl.user.event;

import com.codeit.team5.mopl.global.outbox.event.RetryableOutboxEvent;
import java.util.UUID;

public record UserLockedEvent(UUID id, boolean locked) implements RetryableOutboxEvent {

}
