package com.codeit.team5.mopl.review.controller.api;

import com.codeit.team5.mopl.auth.security.details.MoplPrincipal;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.global.dto.suggestion.ErrorResponseSuggestion;
import com.codeit.team5.mopl.review.dto.request.ReviewCreateRequest;
import com.codeit.team5.mopl.review.dto.request.ReviewGetRequest;
import com.codeit.team5.mopl.review.dto.request.ReviewUpdateRequest;
import com.codeit.team5.mopl.review.dto.response.ReviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "리뷰 관리", description = "리뷰 API")
public interface ReviewApi {

    @Operation(operationId = "getReviews", summary = "리뷰 목록 조회 (커서 페이지네이션)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
        @ApiResponse(responseCode = "401", description = "인증 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class)))
    })
    ResponseEntity<CursorResponse<ReviewResponse>> getReviews(
        @Valid ReviewGetRequest request);

    @Operation(operationId = "createReview", summary = "리뷰 생성",
        description = "생성한 리뷰는 API 요청자 본인의 리뷰로 생성됩니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
        @ApiResponse(responseCode = "401", description = "인증 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
        @ApiResponse(responseCode = "409", description = "이미 리뷰 작성함",
            content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class)))
    })
    ResponseEntity<ReviewResponse> createReview(
        @Parameter(hidden = true) MoplPrincipal moplPrincipal,
        @RequestBody @Valid ReviewCreateRequest request);

    @Operation(operationId = "updateReview", summary = "리뷰 수정")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "수정 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
        @ApiResponse(responseCode = "401", description = "인증 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
        @ApiResponse(responseCode = "403", description = "권한 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
        @ApiResponse(responseCode = "404", description = "리뷰 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class)))
    })
    ResponseEntity<ReviewResponse> updateReview(
        @Parameter(hidden = true) MoplPrincipal moplPrincipal,
        @Parameter(description = "리뷰 ID", required = true)
        @PathVariable UUID reviewId,
        @RequestBody @Valid ReviewUpdateRequest request);

    @Operation(operationId = "deleteReview", summary = "리뷰 삭제")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
        @ApiResponse(responseCode = "403", description = "권한 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
        @ApiResponse(responseCode = "404", description = "리뷰 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class)))
    })
    ResponseEntity<Void> deleteReview(
        @Parameter(hidden = true) MoplPrincipal moplPrincipal,
        @Parameter(description = "리뷰 ID", required = true)
        @PathVariable UUID reviewId);
}
