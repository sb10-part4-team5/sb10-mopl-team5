package com.codeit.team5.mopl.sse;

import java.time.Instant;
import java.util.UUID;

public interface SseEventPayload extends Comparable<SseEventPayload>{
    Instant createdAt();
    UUID eventId();

    @Override
    default int compareTo(SseEventPayload other){
        int cmp = this.createdAt().compareTo(other.createdAt());
        return cmp != 0 ? cmp : this.eventId().compareTo(other.eventId());
    }
}
