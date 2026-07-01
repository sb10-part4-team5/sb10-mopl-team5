package com.codeit.team5.mopl.content.controller.api;

import com.codeit.team5.mopl.content.dto.external.sportsdb.SportsDbLeague;
import com.codeit.team5.mopl.content.dto.request.PageRangeRequest;
import com.codeit.team5.mopl.global.dto.suggestion.ErrorResponseSuggestion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "[어드민] 콘텐츠 수집", description = "외부 API를 통한 콘텐츠 데이터 수집 API")
public interface ContentCollectionApi {

    @Operation(summary = "TMDB 영화 수집", description = "TMDB API에서 인기순으로 영화 데이터를 수집합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "수집 요청 수락 (백그라운드 처리)"),
            @ApiResponse(responseCode = "400", description = "잘못된 페이지 범위",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "403", description = "권한 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class)))
    })
    ResponseEntity<Void> collectTmdbMovies(
            @Valid @ModelAttribute PageRangeRequest request
    );

    @Operation(summary = "TMDB TV 시리즈 수집", description = "TMDB API에서 인기순으로 TV 시리즈 데이터를 수집합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "수집 요청 수락 (백그라운드 처리)"),
            @ApiResponse(responseCode = "400", description = "잘못된 페이지 범위",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "403", description = "권한 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class)))
    })
    ResponseEntity<Void> collectTmdbTvSeries(
            @Valid @ModelAttribute PageRangeRequest request
    );

    @Operation(summary = "SportsDB 경기 수집", description = "SportsDB API에서 리그와 시즌을 지정해 경기 데이터를 수집합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "수집 요청 수락 (백그라운드 처리)"),
            @ApiResponse(responseCode = "400", description = "잘못된 파라미터",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "403", description = "권한 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class)))
    })
    ResponseEntity<Void> collectSportsEvents(
            @Parameter(description = "리그", schema = @Schema(implementation = SportsDbLeague.class))
            @RequestParam SportsDbLeague league,
            @Parameter(description = "시즌 (예: 2023-2024)", example = "2023-2024")
            @Pattern(regexp = "^\\d{4}-\\d{4}$", message = "시즌 형식은 YYYY-YYYY이어야 합니다. (예: 2023-2024)")
            @RequestParam String season
    );

    @Operation(summary = "SportsDB 일별 경기 수집", description = "SportsDB API에서 특정 날짜의 전체 리그 경기 데이터를 수집합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "수집 요청 수락 (백그라운드 처리)"),
            @ApiResponse(responseCode = "400", description = "잘못된 날짜 형식",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "403", description = "권한 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class)))
    })
    ResponseEntity<Void> collectSportsEventsByDay(
            @Parameter(description = "날짜 (예: 2024-12-26)", example = "2024-12-26")
            @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "날짜 형식은 YYYY-MM-DD이어야 합니다. (예: 2024-12-26)")
            @RequestParam String date
    );
}
