package com.codeit.team5.mopl.review.service;

import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.exception.ContentNotFoundException;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.content.service.ContentStatService;
import com.codeit.team5.mopl.global.dto.CursorResponse;

import com.codeit.team5.mopl.review.contant.ReviewSortBy;
import com.codeit.team5.mopl.review.dto.request.ReviewCreateRequest;
import com.codeit.team5.mopl.review.dto.request.ReviewGetRequest;
import com.codeit.team5.mopl.review.dto.request.ReviewUpdateRequest;
import com.codeit.team5.mopl.review.dto.response.ReviewResponse;
import com.codeit.team5.mopl.review.entity.Review;
import com.codeit.team5.mopl.review.exception.CursorIdAfterNotTogetherException;
import com.codeit.team5.mopl.review.exception.ReviewAlreadyExistsException;
import com.codeit.team5.mopl.review.exception.ReviewForbiddenException;
import com.codeit.team5.mopl.review.exception.ReviewNotFoundException;
import com.codeit.team5.mopl.review.mapper.ReviewMapper;
import com.codeit.team5.mopl.review.repository.ReviewRepository;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.exception.UserNotFoundException;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private static final int DEFAULT_LIMIT = 20;
    private static final Direction DEFAULT_DIRECTION = Direction.DESC;
    private static final ReviewSortBy DEFAULT_SORT_BY = ReviewSortBy.CREATED_AT;

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final ReviewMapper reviewMapper;
    private final ContentStatService contentStatService;

    @Transactional(readOnly = true)
    public CursorResponse<ReviewResponse> getReviews(ReviewGetRequest request) {
        UUID contentId = request.contentId();
        String cursor = (request.cursor() != null && !request.cursor().isBlank()) ? request.cursor() : null;
        UUID idAfter = request.idAfter();
        int limit = request.limit() != null ? request.limit() : DEFAULT_LIMIT;
        Direction direction = request.sortDirection() != null ? request.sortDirection() : DEFAULT_DIRECTION;
        ReviewSortBy sortBy = request.sortBy() != null ? request.sortBy() : DEFAULT_SORT_BY;

        validateCursorIdAfterPair(cursor, idAfter);

        Limit fetchLimit = Limit.of(limit + 1);

        List<Review> rows;

        if (sortBy == ReviewSortBy.CREATED_AT) {
            Instant createdAtCursor = null;
            if (cursor != null) {
                createdAtCursor = parseInstantCursor(cursor);
            }
            rows = reviewRepository.findPageByContentIdSortByCreatedAt(contentId, createdAtCursor, idAfter, fetchLimit, direction);
        } else {
            Double ratingCursor = null;
            if (cursor != null) {
                ratingCursor = parseDoubleCursor(cursor);
            }
            rows = reviewRepository.findPageByContentIdSortByRating(contentId, ratingCursor, idAfter, fetchLimit, direction);
        }

        boolean hasNext = rows.size() > limit;
        List<Review> page = hasNext ? rows.subList(0, limit) : rows;

        String nextCursor = null;
        String nextIdAfter = null;
        if (hasNext && !page.isEmpty()) {
            Review last = page.get(page.size() - 1);
            nextCursor = sortBy == ReviewSortBy.RATING
                ? last.getRating().toString()
                : last.getCreatedAt().toString();
            nextIdAfter = last.getId().toString();
        }

        long totalCount = reviewRepository.countByContent_Id(contentId);

        List<ReviewResponse> data = page.stream()
            .map(reviewMapper::toDto)
            .toList();

        return new CursorResponse<>(data, nextCursor, nextIdAfter, hasNext, totalCount,
            sortBy.getSortByType(), direction.toString());
    }

    @Transactional
    public ReviewResponse createReview(UUID authorId, ReviewCreateRequest request) {
        Content content = contentRepository.findById(request.contentId())
            .orElseThrow(() -> new ContentNotFoundException(request.contentId()));
        User author = userRepository.findById(authorId)
            .orElseThrow(() -> new UserNotFoundException(authorId));

        if (reviewRepository.existsByContent_IdAndAuthor_Id(request.contentId(), authorId)) {
            throw new ReviewAlreadyExistsException();
        }

        Review saved = reviewRepository.save(Review.of(content, author, request.text(), request.rating()));
        log.info("리뷰 생성 완료: reviewId={}, contentId={}, authorId={}", saved.getId(), saved.getContentId(), authorId);

        contentStatService.updateContentStat(request.contentId(), request.rating(), 1);
        return reviewMapper.toDto(saved);
    }

    @Transactional
    public ReviewResponse updateReview(UUID reviewId, UUID authorId, ReviewUpdateRequest request) {
        Review review = reviewRepository.findByIdWithAuthor(reviewId)
            .orElseThrow(() -> new ReviewNotFoundException(reviewId));
        if (!review.getAuthorId().equals(authorId)) {
            log.warn("리뷰 수정 권한 없음: reviewId={}, requesterId={}", reviewId, authorId);
            throw new ReviewForbiddenException();
        }
        double oldRating = review.getRating();
        review.update(request.text(), request.rating());
        contentStatService.updateContentStat(review.getContentId(), request.rating() - oldRating, 0);
        log.info("리뷰 수정 완료: reviewId={}, authorId={}", reviewId, authorId);
        return reviewMapper.toDto(review);
    }

    @Transactional
    public void deleteReview(UUID reviewId, UUID authorId) {
        Review review = reviewRepository.findByIdWithAuthor(reviewId)
            .orElseThrow(() -> new ReviewNotFoundException(reviewId));
        if (!review.getAuthorId().equals(authorId)) {
            log.warn("리뷰 삭제 권한 없음: reviewId={}, requesterId={}", reviewId, authorId);
            throw new ReviewForbiddenException();
        }
        contentStatService.updateContentStat(review.getContentId(), -review.getRating(), -1);
        reviewRepository.delete(review);
        log.info("리뷰 삭제 완료: reviewId={}, authorId={}", reviewId, authorId);
    }

    private void validateCursorIdAfterPair(String cursor, UUID idAfter) {
        if ((cursor != null && idAfter == null) || (cursor == null && idAfter != null)) {
            throw new CursorIdAfterNotTogetherException();
        }
    }

    private Instant parseInstantCursor(String cursor) {
        return (Instant) ReviewSortBy.CREATED_AT.parse(cursor);
    }

    private Double parseDoubleCursor(String cursor) {
        return (Double) ReviewSortBy.RATING.parse(cursor);
    }
}
