package com.codeit.team5.mopl.content.client.tmdb;

import com.codeit.team5.mopl.content.dto.external.tmdb.TmdbMovieListResponse;
import com.codeit.team5.mopl.content.dto.external.tmdb.TmdbTvListResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class TmdbApiClient {

    private final WebClient tmdbWebClient;

    public TmdbApiClient(@Qualifier("tmdbWebClient") WebClient tmdbWebClient) {
        this.tmdbWebClient = tmdbWebClient;
    }

    public TmdbMovieListResponse fetchMovies(int page) {
        return tmdbWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/discover/movie")
                        .queryParam("language", "ko-KR")
                        .queryParam("sort_by", "popularity.desc")
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .bodyToMono(TmdbMovieListResponse.class)
                .block();
    }

    public TmdbTvListResponse fetchTvSeries(int page) {
        return tmdbWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/discover/tv")
                        .queryParam("language", "ko-KR")
                        .queryParam("sort_by", "popularity.desc")
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .bodyToMono(TmdbTvListResponse.class)
                .block();
    }
}
