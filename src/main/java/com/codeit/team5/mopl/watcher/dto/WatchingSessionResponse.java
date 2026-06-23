package com.codeit.team5.mopl.watcher.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record WatchingSessionResponse(UUID id,
                                      Instant createdAt,
                                      WatcherDto watcher,
                                      WatchingContentDto content) {

}
