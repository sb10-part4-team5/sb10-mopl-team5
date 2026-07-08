package com.codeit.team5.mopl.playlist.event;

import java.util.UUID;
import org.springframework.context.ApplicationEvent;

public record PlaylistCreatedEvent(UUID id) {

}
