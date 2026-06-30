package com.codeit.team5.mopl.content.controller;

import com.codeit.team5.mopl.content.controller.api.ContentCollectionApi;
import com.codeit.team5.mopl.content.dto.external.sportsdb.SportsDbLeague;
import com.codeit.team5.mopl.content.service.SportsDbContentService;
import com.codeit.team5.mopl.content.service.TmdbContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/admin/contents/collect")
@RequiredArgsConstructor
public class ContentCollectionController implements ContentCollectionApi {

    private final TmdbContentService tmdbContentService;
    private final SportsDbContentService sportsDbContentService;

    @PostMapping("/tmdb/movies")
    public ResponseEntity<Void> collectTmdbMovies(
            @RequestParam(defaultValue = "1") int startPage,
            @RequestParam(defaultValue = "1") int endPage
    ) {
        log.info("TMDB 영화 수집 요청: {}~{}페이지", startPage, endPage);
        tmdbContentService.collectMovies(startPage, endPage);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tmdb/tv")
    public ResponseEntity<Void> collectTmdbTvSeries(
            @RequestParam(defaultValue = "1") int startPage,
            @RequestParam(defaultValue = "1") int endPage
    ) {
        log.info("TMDB TV 시리즈 수집 요청: {}~{}페이지", startPage, endPage);
        tmdbContentService.collectTvSeries(startPage, endPage);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sports")
    public ResponseEntity<Void> collectSportsEvents(
            @RequestParam SportsDbLeague league,
            @RequestParam String season
    ) {
        log.info("SportsDB 경기 수집 요청: league={}, season={}", league.getName(), season);
        sportsDbContentService.collectEvents(league.getLeagueId(), season);
        return ResponseEntity.ok().build();
    }
}
