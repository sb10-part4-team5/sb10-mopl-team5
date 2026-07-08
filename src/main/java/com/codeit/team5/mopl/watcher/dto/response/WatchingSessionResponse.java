package com.codeit.team5.mopl.watcher.dto.response;

import com.codeit.team5.mopl.content.dto.response.ContentResponse;
import com.codeit.team5.mopl.user.dto.response.UserSummaryResponse;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record WatchingSessionResponse(UUID id, Instant createdAt, UserSummaryResponse watcher,
                                      ContentResponse content) {

}
