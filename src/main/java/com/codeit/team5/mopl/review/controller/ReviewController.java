package com.codeit.team5.mopl.review.controller;

import com.codeit.team5.mopl.auth.security.details.MoplPrincipal;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.review.controller.api.ReviewApi;
import com.codeit.team5.mopl.review.dto.request.ReviewCreateRequest;
import com.codeit.team5.mopl.review.dto.request.ReviewGetRequest;
import com.codeit.team5.mopl.review.dto.request.ReviewUpdateRequest;
import com.codeit.team5.mopl.review.dto.response.ReviewResponse;
import com.codeit.team5.mopl.review.service.ReviewService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/reviews")
public class ReviewController implements ReviewApi {

    private final ReviewService reviewService;

    @Override
    @GetMapping
    public ResponseEntity<CursorResponse<ReviewResponse>> getReviews(@Valid ReviewGetRequest request) {
        CursorResponse<ReviewResponse> response = reviewService.getReviews(request);

        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
        @AuthenticationPrincipal MoplPrincipal moplPrincipal,
        @RequestBody @Valid ReviewCreateRequest request) {
        ReviewResponse response = reviewService.createReview(moplPrincipal.getId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @PatchMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(
        @AuthenticationPrincipal MoplPrincipal moplPrincipal,
        @PathVariable UUID reviewId,
        @RequestBody @Valid ReviewUpdateRequest request) {
        ReviewResponse response = reviewService.updateReview(reviewId, moplPrincipal.getId(), request);

        return ResponseEntity.ok(response);
    }

    @Override
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
        @AuthenticationPrincipal MoplPrincipal moplPrincipal,
        @PathVariable UUID reviewId) {
        reviewService.deleteReview(reviewId, moplPrincipal.getId());

        return ResponseEntity.noContent().build();
    }
}
