package com.codeit.team5.mopl.content.dto.external.sportsdb;

import java.util.List;
import lombok.Getter;

@Getter
public enum SportsDbLeague {
    EPL("4328", "English Premier League"),
    EFL_CHAMPIONSHIP("4329", "English League Championship"),
    SCOTTISH_PREMIER("4330", "Scottish Premier League"),
    BUNDESLIGA("4331", "German Bundesliga"),
    SERIE_A("4332", "Italian Serie A"),
    LIGUE_1("4334", "French Ligue 1"),
    LA_LIGA("4335", "Spanish La Liga"),
    GREEK_SUPER_LEAGUE("4336", "Greek Super League 1"),
    EREDIVISIE("4337", "Dutch Eredivisie"),
    BELGIAN_PRO_LEAGUE("4338", "Belgian Pro League");

    private static final List<String> SEASONS = List.of("2023-2024", "2024-2025", "2025-2026");

    private final String leagueId;
    private final String name;

    SportsDbLeague(String leagueId, String name) {
        this.leagueId = leagueId;
        this.name = name;
    }

    public static List<String> getSeasons() {
        return SEASONS;
    }
}
