package com.codeit.team5.mopl.content.dto.external.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record TmdbTvListResponse(
        int page,
        List<TmdbTvDto> results,
        @JsonProperty("total_pages") int totalPages,
        @JsonProperty("total_results") int totalResults
) {
}
