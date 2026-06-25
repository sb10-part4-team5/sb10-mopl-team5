package com.codeit.team5.mopl.watcher.dto.response;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record WatchingSessionResponse(UUID id,
                                      Instant createdAt,
                                      WatcherResponse watcher,
                                      WatchingContentResponse content) {

}
