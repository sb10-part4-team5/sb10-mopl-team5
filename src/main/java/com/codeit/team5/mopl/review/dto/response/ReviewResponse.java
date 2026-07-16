package com.codeit.team5.mopl.review.dto.response;

import com.codeit.team5.mopl.user.dto.response.UserSummary;
import java.util.UUID;

public record ReviewResponse(
    UUID id,
    UUID contentId,
    UserSummary author,
    String text,
    Double rating
) {
}
