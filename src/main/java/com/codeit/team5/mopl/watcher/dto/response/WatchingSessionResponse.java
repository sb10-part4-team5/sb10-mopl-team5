package com.codeit.team5.mopl.watcher.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record WatchingSessionResponse(UUID id,
                                      Instant createdAt,
                                      @JsonProperty("user") WatcherResponse watcher,
                                      WatchingContentResponse content) {

}
