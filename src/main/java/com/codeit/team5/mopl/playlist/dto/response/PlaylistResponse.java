package com.codeit.team5.mopl.playlist.dto.response;

import com.codeit.team5.mopl.user.dto.response.UserSummary;
import java.time.Instant;
import java.util.UUID;

public record PlaylistResponse(UUID id,
                               UserSummary owner,
                               Instant updatedAt,
                               String title,
                               String description,
                               Integer subscriberCount,
                               boolean subscribedByMe) {

}
