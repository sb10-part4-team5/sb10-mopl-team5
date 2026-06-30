package com.codeit.team5.mopl.content.dto.external.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record TmdbTvDto(
        long id,
        String name,
        String overview,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("genre_ids") List<Long> genreIds,
        @JsonProperty("first_air_date") String firstAirDate,
        @JsonProperty("vote_average") double voteAverage,
        @JsonProperty("original_language") String originalLanguage
) {
}
