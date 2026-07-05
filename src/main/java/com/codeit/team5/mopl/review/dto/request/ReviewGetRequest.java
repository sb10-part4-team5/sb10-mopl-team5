package com.codeit.team5.mopl.review.dto.request;

import com.codeit.team5.mopl.review.contant.ReviewSortBy;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.data.domain.Sort;

public record ReviewGetRequest(
    @NotNull
    UUID contentId,

    String cursor,

    UUID idAfter,

    @Min(1)
    @Max(100)
    Integer limit,

    Sort.Direction sortDirection,

    ReviewSortBy sortBy
) {
}
