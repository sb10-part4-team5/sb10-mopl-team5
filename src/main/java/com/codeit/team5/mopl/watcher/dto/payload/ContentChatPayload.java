package com.codeit.team5.mopl.watcher.dto.payload;

import com.codeit.team5.mopl.user.dto.response.UserSummary;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ContentChatPayload(@JsonProperty("sender") UserSummary watcher,
                                 String content) {

}
