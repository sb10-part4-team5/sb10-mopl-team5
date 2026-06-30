package com.codeit.team5.mopl.content.controller.api;

import com.codeit.team5.mopl.content.dto.external.sportsdb.SportsDbLeague;
import com.codeit.team5.mopl.global.dto.suggestion.ErrorResponseSuggestion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "[어드민] 콘텐츠 수집", description = "외부 API를 통한 콘텐츠 데이터 수집 API")
public interface ContentCollectionApi {

    @Operation(summary = "TMDB 영화 수집", description = "TMDB API에서 인기순으로 영화 데이터를 수집합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "수집 요청 수락 (백그라운드 처리)"),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "403", description = "권한 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class)))
    })
    ResponseEntity<Void> collectTmdbMovies(
            @Parameter(description = "시작 페이지", example = "1") int startPage,
            @Parameter(description = "종료 페이지", example = "5") int endPage
    );

    @Operation(summary = "TMDB TV 시리즈 수집", description = "TMDB API에서 인기순으로 TV 시리즈 데이터를 수집합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "수집 요청 수락 (백그라운드 처리)"),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "403", description = "권한 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class)))
    })
    ResponseEntity<Void> collectTmdbTvSeries(
            @Parameter(description = "시작 페이지", example = "1") int startPage,
            @Parameter(description = "종료 페이지", example = "5") int endPage
    );

    @Operation(summary = "SportsDB 경기 수집", description = "SportsDB API에서 리그와 시즌을 지정해 경기 데이터를 수집합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "수집 요청 수락 (백그라운드 처리)"),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "403", description = "권한 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class)))
    })
    ResponseEntity<Void> collectSportsEvents(
            @Parameter(description = "리그", schema = @Schema(implementation = SportsDbLeague.class)) SportsDbLeague league,
            @Parameter(description = "시즌 (예: 2023-2024)", example = "2023-2024") String season
    );
}
