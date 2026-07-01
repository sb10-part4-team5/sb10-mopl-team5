package com.codeit.team5.mopl.content.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@ValidPageRange
public record PageRangeRequest(
        @NotNull @Min(1) Integer startPage,
        @NotNull @Min(1) Integer endPage
) {}
