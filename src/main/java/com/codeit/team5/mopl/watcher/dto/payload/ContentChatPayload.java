package com.codeit.team5.mopl.watcher.dto.payload;

import com.codeit.team5.mopl.watcher.dto.response.WatcherResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ContentChatPayload(@JsonProperty("sender") WatcherResponse watcher,
                                 String content) {

}
