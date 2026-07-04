package com.codeit.team5.mopl.watcher.dto.response;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import com.codeit.team5.mopl.user.dto.response.UserSummary;

@Builder
public record WatchingSessionResponse(UUID id, Instant createdAt, UserSummary watcher,
        WatchingContentResponse content) {

}
