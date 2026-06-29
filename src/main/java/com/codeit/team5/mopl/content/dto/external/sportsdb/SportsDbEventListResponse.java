package com.codeit.team5.mopl.content.dto.external.sportsdb;

import java.util.List;

public record SportsDbEventListResponse(
        List<SportsDbEventDto> events
) {
}
