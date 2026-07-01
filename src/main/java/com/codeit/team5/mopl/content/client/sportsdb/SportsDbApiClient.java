package com.codeit.team5.mopl.content.client.sportsdb;

import com.codeit.team5.mopl.content.dto.external.sportsdb.SportsDbEventListResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class SportsDbApiClient {

    private final WebClient sportsDbWebClient;

    public SportsDbApiClient(@Qualifier("sportsDbWebClient") WebClient sportsDbWebClient) {
        this.sportsDbWebClient = sportsDbWebClient;
    }

    public SportsDbEventListResponse fetchEventsBySeason(String leagueId, String season) {
        return sportsDbWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/eventsseason.php")
                        .queryParam("id", leagueId)
                        .queryParam("s", season)
                        .build())
                .retrieve()
                .bodyToMono(SportsDbEventListResponse.class)
                .block();
    }

    public SportsDbEventListResponse fetchEventsByDay(String leagueId, String date) {
        return sportsDbWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/eventsday.php")
                        .queryParam("d", date)
                        .queryParam("l", leagueId)
                        .build())
                .retrieve()
                .bodyToMono(SportsDbEventListResponse.class)
                .block();
    }
}
