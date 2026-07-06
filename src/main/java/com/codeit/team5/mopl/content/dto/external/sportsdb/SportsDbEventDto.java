package com.codeit.team5.mopl.content.dto.external.sportsdb;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SportsDbEventDto(
        @JsonProperty("idEvent") String idEvent,
        @JsonProperty("strEvent") String strEvent,
        @JsonProperty("strThumb") String strThumb,
        @JsonProperty("dateEvent") String dateEvent,
        @JsonProperty("strLeague") String strLeague,
        @JsonProperty("strSport") String strSport,
        @JsonProperty("strHomeTeam") String strHomeTeam,
        @JsonProperty("strAwayTeam") String strAwayTeam,
        @JsonProperty("intHomeScore") String intHomeScore,
        @JsonProperty("intAwayScore") String intAwayScore
) {
}
