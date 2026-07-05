package com.codeit.team5.mopl.review.controller;

import com.codeit.team5.mopl.auth.security.details.MoplPrincipal;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.review.controller.api.ReviewApi;
import com.codeit.team5.mopl.review.dto.request.ReviewCreateRequest;
import com.codeit.team5.mopl.review.dto.request.ReviewUpdateRequest;
import com.codeit.team5.mopl.review.dto.response.ReviewResponse;
import com.codeit.team5.mopl.review.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@Slf4j
@RequestMapping("/api/reviews")
public class ReviewController implements ReviewApi {

    private final ReviewService reviewService;

    @Override
    @GetMapping
    public ResponseEntity<CursorResponse<ReviewResponse>> getReviews(
        @RequestParam UUID contentId,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
        @RequestParam(defaultValue = "DESCENDING") String sortDirection,
        @RequestParam(defaultValue = "createdAt") String sortBy) {

        log.info("리뷰 목록 조회: GET /api/reviews, contentId={}", contentId);

        CursorResponse<ReviewResponse> response = reviewService.getReviews(
            contentId, cursor, idAfter, limit, sortDirection, sortBy);

        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
        @AuthenticationPrincipal MoplPrincipal moplPrincipal,
        @RequestBody @Valid ReviewCreateRequest request) {

        log.info("리뷰 생성: POST /api/reviews, authorId={}", moplPrincipal.getId());

        ReviewResponse response = reviewService.createReview(moplPrincipal.getId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @PatchMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(
        @AuthenticationPrincipal MoplPrincipal moplPrincipal,
        @PathVariable UUID reviewId,
        @RequestBody @Valid ReviewUpdateRequest request) {

        log.info("리뷰 수정: PATCH /api/reviews/{}, authorId={}", reviewId, moplPrincipal.getId());

        ReviewResponse response = reviewService.updateReview(reviewId, moplPrincipal.getId(), request);

        return ResponseEntity.ok(response);
    }

    @Override
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
        @AuthenticationPrincipal MoplPrincipal moplPrincipal,
        @PathVariable UUID reviewId) {

        log.info("리뷰 삭제: DELETE /api/reviews/{}, authorId={}", reviewId, moplPrincipal.getId());

        reviewService.deleteReview(reviewId, moplPrincipal.getId());

        return ResponseEntity.noContent().build();
    }
}
