package com.codeit.team5.mopl.review.service;

import com.codeit.team5.mopl.content.exception.ContentNotFoundException;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.global.exception.InvalidSortDirectionException;
import com.codeit.team5.mopl.review.dto.request.ReviewCreateRequest;
import com.codeit.team5.mopl.review.dto.request.ReviewUpdateRequest;
import com.codeit.team5.mopl.review.dto.response.ReviewResponse;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.review.entity.Review;
import com.codeit.team5.mopl.review.exception.InvalidReviewSortByException;
import com.codeit.team5.mopl.review.exception.ReviewAlreadyExistsException;
import com.codeit.team5.mopl.review.exception.ReviewForbiddenException;
import com.codeit.team5.mopl.review.exception.ReviewNotFoundException;
import com.codeit.team5.mopl.review.mapper.ReviewMapper;
import com.codeit.team5.mopl.review.repository.ReviewRepository;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.exception.UserNotFoundException;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private static final String SORT_BY_CREATED_AT = "createdAt";
    private static final String SORT_BY_RATING = "rating";
    private static final String SORT_ASCENDING = "ASCENDING";
    private static final String SORT_DESCENDING = "DESCENDING";

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final ReviewMapper reviewMapper;

    // 리뷰 목록 조회
    @Transactional(readOnly = true)
    public CursorResponse<ReviewResponse> getReviews(
        UUID contentId, String cursor, UUID idAfter, int limit,
        String sortDirection, String sortBy) {

        boolean ascending = resolveAscending(sortBy, sortDirection);
        Limit fetchLimit = Limit.of(limit + 1); // LIMIT + 1 만큼 fetch 하여 다음 페이지 여부를 확인하기 위함

        List<Review> rows = reviewRepository.findPageByContentId(
            contentId, cursor, idAfter, fetchLimit, sortBy, ascending);


        boolean hasNext = rows.size() > limit; // 다음 페이지 여부
        // 다음 페이지가 있으면 row에서 limit 만큼 fetch
        // 다음 페이지 없으면 row 다 fetch
        List<Review> page = hasNext ? rows.subList(0, limit) : rows;

        String nextCursor = null;
        String nextIdAfter = null;

        if (hasNext && !page.isEmpty()) {
            Review last = page.get(page.size() - 1); // 페이지의 마지막 item
            nextCursor = SORT_BY_RATING.equals(sortBy)
                ? last.getRating().toString() // 평점 기준일 때 마지막 item의 평점을 cursor로
                : last.getCreatedAt().toString(); // createdAt
            nextIdAfter = last.getId().toString(); // 보조 커서는 id
        }

        // 총 개수
        long totalCount = reviewRepository.countByContent_Id(contentId);

        // 페이지의 authorId를 한 번에 모아서 배치 조회 (N+1 방지)
        Map<UUID, User> userMap = userRepository.findAllById(
                page.stream().map(Review::getAuthorId).toList()
            ).stream()
            .collect(Collectors.toMap(User::getId, u -> u));

        List<ReviewResponse> data = page.stream()
            .map(review -> reviewMapper.toDto(review, userMap.get(review.getAuthorId())))
            .toList();

        return new CursorResponse<>(data, nextCursor, nextIdAfter, hasNext, totalCount, sortBy, sortDirection);
    }

    // 리뷰 생성
    @Transactional
    public ReviewResponse createReview(UUID authorId, ReviewCreateRequest request) {
        Content content = contentRepository.findById(request.contentId())
            .orElseThrow(() -> new ContentNotFoundException(request.contentId()));

        // 해당 유저의 리뷰가 이미 존재하면 예외 던지기
        if (reviewRepository.existsByContent_IdAndAuthorId(request.contentId(), authorId)) {
            throw new ReviewAlreadyExistsException();
        }
        Review review = Review.of(content, authorId, request.text(), request.rating());
        Review saved;
        try {
            saved = reviewRepository.saveAndFlush(review);
        } catch (DataIntegrityViolationException e) {
            log.warn("리뷰 중복 저장 시도 (race condition): contentId={}, authorId={}", request.contentId(), authorId);
            throw new ReviewAlreadyExistsException();
        }
        log.info("리뷰 생성 완료: reviewId={}, contentId={}, authorId={}", saved.getId(), saved.getContentId(), authorId);

        User user = userRepository.findById(saved.getAuthorId()).orElseThrow(() -> new UserNotFoundException(authorId));

        return reviewMapper.toDto(saved, user);
    }

    // 리뷰 수정
    @Transactional
    public ReviewResponse updateReview(UUID reviewId, UUID authorId, ReviewUpdateRequest request) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ReviewNotFoundException(reviewId));
        if (!review.getAuthorId().equals(authorId)) {
            log.warn("리뷰 수정 권한 없음: reviewId={}, requesterId={}", reviewId, authorId);
            throw new ReviewForbiddenException();
        }
        review.update(request.text(), request.rating());
        log.info("리뷰 수정 완료: reviewId={}, authorId={}", reviewId, authorId);
        User author = userRepository.findById(authorId)
            .orElseThrow(() -> new UserNotFoundException(authorId));
        return reviewMapper.toDto(review, author);
    }

    // 리뷰 삭제
    @Transactional
    public void deleteReview(UUID reviewId, UUID authorId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ReviewNotFoundException(reviewId));
        if (!review.getAuthorId().equals(authorId)) {
            log.warn("리뷰 삭제 권한 없음: reviewId={}, requesterId={}", reviewId, authorId);
            throw new ReviewForbiddenException();
        }
        reviewRepository.delete(review);
        log.info("리뷰 삭제 완료: reviewId={}, authorId={}", reviewId, authorId);
    }

    // SortBy, SortDirection을 검증하고 ASC/DESC 여부를 정하는 헬퍼 메서드
    private boolean resolveAscending(String sortBy, String sortDirection) {
        if (!SORT_BY_CREATED_AT.equals(sortBy) && !SORT_BY_RATING.equals(sortBy)) {
            throw new InvalidReviewSortByException(sortBy); // 평점 순, createdAt 순도 아닌 이상한게 뭐가 들어왔어
        }
        if (SORT_ASCENDING.equalsIgnoreCase(sortDirection)) {
            return true;
        }
        if (SORT_DESCENDING.equalsIgnoreCase(sortDirection)) {
            return false;
        }
        throw new InvalidSortDirectionException(sortDirection);
    }
}
