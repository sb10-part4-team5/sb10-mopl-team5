package com.codeit.team5.mopl.review.dto.response;

import com.codeit.team5.mopl.user.dto.response.UserSummaryResponse;
import java.util.UUID;

public record ReviewResponse(
    UUID id,
    UUID contentId,
    UserSummaryResponse author,
    String text,
    Double rating
) {
}
