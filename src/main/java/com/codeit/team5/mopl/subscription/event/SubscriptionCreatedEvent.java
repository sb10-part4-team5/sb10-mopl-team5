package com.codeit.team5.mopl.subscription.event;

import java.util.UUID;
import org.springframework.context.ApplicationEvent;

public record SubscriptionCreatedEvent(UUID playlistId, UUID userId) {

}
