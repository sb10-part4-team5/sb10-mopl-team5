package com.codeit.team5.mopl.watcher.dto.payload;

import com.codeit.team5.mopl.watcher.constant.WatcherStatus;
import com.codeit.team5.mopl.watcher.dto.response.WatchingSessionResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

public record WatchingSessionPayload(@JsonProperty("type") WatcherStatus status,
                                     @JsonProperty("watchingSession") WatchingSessionResponse response,
                                     long watcherCount) {

}
