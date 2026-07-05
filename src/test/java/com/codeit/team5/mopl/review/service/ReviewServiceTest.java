package com.codeit.team5.mopl.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.exception.ContentNotFoundException;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.review.contant.ReviewSortBy;
import com.codeit.team5.mopl.review.dto.request.ReviewCreateRequest;
import com.codeit.team5.mopl.review.dto.request.ReviewGetRequest;
import com.codeit.team5.mopl.review.dto.request.ReviewUpdateRequest;
import com.codeit.team5.mopl.review.dto.response.ReviewResponse;
import com.codeit.team5.mopl.review.entity.Review;
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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    @DisplayName("다음 페이지가 있으면 limit만큼 자르고 createdAt 기준 nextCursor를 채운다")
    void getReviews_hasNext_createdAtCursor() {
        // given
        UUID contentId = UUID.randomUUID();
        Instant lastCreatedAt = Instant.parse("2026-01-01T00:00:00Z");
        UUID lastId = UUID.randomUUID();

        Review r0 = mock(Review.class);
        Review r1 = mock(Review.class);
        Review r2 = mock(Review.class);
        given(r1.getCreatedAt()).willReturn(lastCreatedAt);
        given(r1.getId()).willReturn(lastId);

        ReviewGetRequest request = new ReviewGetRequest(contentId, null, null, 2, Sort.Direction.DESC, ReviewSortBy.CREATED_AT);
        given(reviewRepository.findPageByContentIdSortByCreatedAt(contentId, null, null, Limit.of(3), Sort.Direction.DESC))
            .willReturn(List.of(r0, r1, r2));
        given(reviewRepository.countByContent_Id(contentId)).willReturn(5L);
        given(reviewMapper.toDto(any(Review.class))).willReturn(mock(ReviewResponse.class));

        // when
        CursorResponse<ReviewResponse> result = reviewService.getReviews(request);

        // then
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(lastCreatedAt.toString());
        assertThat(result.nextIdAfter()).isEqualTo(lastId.toString());
        assertThat(result.totalCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("다음 페이지가 있으면 rating 기준 nextCursor를 채운다")
    void getReviews_hasNext_ratingCursor() {
        // given
        UUID contentId = UUID.randomUUID();
        double lastRating = 4.5;
        UUID lastId = UUID.randomUUID();

        Review r0 = mock(Review.class);
        Review r1 = mock(Review.class);
        Review r2 = mock(Review.class);
        given(r1.getRating()).willReturn(lastRating);
        given(r1.getId()).willReturn(lastId);

        ReviewGetRequest request = new ReviewGetRequest(contentId, null, null, 2, Sort.Direction.DESC, ReviewSortBy.RATING);
        given(reviewRepository.findPageByContentIdSortByRating(contentId, null, null, Limit.of(3), Sort.Direction.DESC))
            .willReturn(List.of(r0, r1, r2));
        given(reviewRepository.countByContent_Id(contentId)).willReturn(5L);
        given(reviewMapper.toDto(any(Review.class))).willReturn(mock(ReviewResponse.class));

        // when
        CursorResponse<ReviewResponse> result = reviewService.getReviews(request);

        // then
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(String.valueOf(lastRating));
        assertThat(result.nextIdAfter()).isEqualTo(lastId.toString());
    }

    @Test
    @DisplayName("마지막 페이지면 hasNext가 false이고 nextCursor가 null이다")
    void getReviews_lastPage() {
        // given
        UUID contentId = UUID.randomUUID();

        Review r0 = mock(Review.class);
        ReviewGetRequest request = new ReviewGetRequest(contentId, null, null, 2, Sort.Direction.DESC, ReviewSortBy.CREATED_AT);
        given(reviewRepository.findPageByContentIdSortByCreatedAt(contentId, null, null, Limit.of(3), Sort.Direction.DESC))
            .willReturn(List.of(r0));
        given(reviewRepository.countByContent_Id(contentId)).willReturn(1L);
        given(reviewMapper.toDto(any(Review.class))).willReturn(mock(ReviewResponse.class));

        // when
        CursorResponse<ReviewResponse> result = reviewService.getReviews(request);

        // then
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
        assertThat(result.nextIdAfter()).isNull();
    }

    @Test
    @DisplayName("sortDirection이 ASCENDING이면 ASC로 리포지토리를 호출한다")
    void getReviews_ascending() {
        // given
        UUID contentId = UUID.randomUUID();

        Review r0 = mock(Review.class);
        ReviewGetRequest request = new ReviewGetRequest(contentId, null, null, 2, Sort.Direction.ASC, ReviewSortBy.CREATED_AT);
        given(reviewRepository.findPageByContentIdSortByCreatedAt(contentId, null, null, Limit.of(3), Sort.Direction.ASC))
            .willReturn(List.of(r0));
        given(reviewRepository.countByContent_Id(contentId)).willReturn(1L);
        given(reviewMapper.toDto(any(Review.class))).willReturn(mock(ReviewResponse.class));

        // when
        CursorResponse<ReviewResponse> result = reviewService.getReviews(request);

        // then
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("리뷰 생성에 성공한다")
    void ofReview_success() {
        // given
        UUID authorId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        ReviewCreateRequest request = new ReviewCreateRequest(contentId, "좋아요", 4.5);

        Content content = mock(Content.class);
        User user = mock(User.class);
        Review saved = mock(Review.class);
        ReviewResponse response = mock(ReviewResponse.class);

        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
        given(userRepository.findById(authorId)).willReturn(Optional.of(user));
        given(reviewRepository.existsByContent_IdAndAuthor_Id(contentId, authorId)).willReturn(false);
        given(reviewRepository.save(any())).willReturn(saved);
        given(saved.getId()).willReturn(UUID.randomUUID());
        given(saved.getContentId()).willReturn(contentId);
        given(reviewMapper.toDto(saved)).willReturn(response);

        // when
        ReviewResponse result = reviewService.createReview(authorId, request);

        // then
        assertThat(result).isEqualTo(response);
        verify(reviewRepository).save(any());
    }

    @Test
    @DisplayName("콘텐츠가 존재하지 않으면 리뷰 생성 시 예외가 발생한다")
    void ofReview_contentNotFound_exception() {
        // given
        UUID authorId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        ReviewCreateRequest request = new ReviewCreateRequest(contentId, "좋아요", 4.5);

        given(contentRepository.findById(contentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(authorId, request))
            .isInstanceOf(ContentNotFoundException.class);
    }

    @Test
    @DisplayName("이미 리뷰를 작성했으면 예외가 발생한다")
    void ofReview_alreadyExists_exception() {
        // given
        UUID authorId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        ReviewCreateRequest request = new ReviewCreateRequest(contentId, "좋아요", 4.5);

        given(contentRepository.findById(contentId)).willReturn(Optional.of(mock(Content.class)));
        given(userRepository.findById(authorId)).willReturn(Optional.of(mock(User.class)));
        given(reviewRepository.existsByContent_IdAndAuthor_Id(contentId, authorId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(authorId, request))
            .isInstanceOf(ReviewAlreadyExistsException.class);
    }

    @Test
    @DisplayName("존재하지 않는 유저가 리뷰 생성 시 예외가 발생한다")
    void ofReview_userNotFound_exception() {
        // given
        UUID authorId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        ReviewCreateRequest request = new ReviewCreateRequest(contentId, "좋아요", 4.5);

        given(contentRepository.findById(contentId)).willReturn(Optional.of(mock(Content.class)));
        given(userRepository.findById(authorId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(authorId, request))
            .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("리뷰 수정에 성공한다")
    void updateReview_success() {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        ReviewUpdateRequest request = new ReviewUpdateRequest("수정된 내용", 3.0);

        Review review = mock(Review.class);
        ReviewResponse response = mock(ReviewResponse.class);

        given(reviewRepository.findByIdWithAuthor(reviewId)).willReturn(Optional.of(review));
        given(review.getAuthorId()).willReturn(authorId);
        given(reviewMapper.toDto(review)).willReturn(response);

        // when
        ReviewResponse result = reviewService.updateReview(reviewId, authorId, request);

        // then
        assertThat(result).isEqualTo(response);
        verify(review).update("수정된 내용", 3.0);
    }

    @Test
    @DisplayName("존재하지 않는 리뷰 수정 시 예외가 발생한다")
    void updateReview_notFound_exception() {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        given(reviewRepository.findByIdWithAuthor(reviewId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.updateReview(
            reviewId, authorId, new ReviewUpdateRequest(null, null)))
            .isInstanceOf(ReviewNotFoundException.class);
    }

    @Test
    @DisplayName("본인의 리뷰가 아니면 수정 시 예외가 발생한다")
    void updateReview_forbidden_exception() {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();

        Review review = mock(Review.class);
        given(reviewRepository.findByIdWithAuthor(reviewId)).willReturn(Optional.of(review));
        given(review.getAuthorId()).willReturn(otherId);

        // when & then
        assertThatThrownBy(() -> reviewService.updateReview(
            reviewId, authorId, new ReviewUpdateRequest("수정내용", 4.0)))
            .isInstanceOf(ReviewForbiddenException.class);
    }

    @Test
    @DisplayName("리뷰 삭제에 성공한다")
    void deleteReview_success() {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        Review review = mock(Review.class);
        given(reviewRepository.findByIdWithAuthor(reviewId)).willReturn(Optional.of(review));
        given(review.getAuthorId()).willReturn(authorId);

        // when
        reviewService.deleteReview(reviewId, authorId);

        // then
        verify(reviewRepository).delete(review);
    }

    @Test
    @DisplayName("존재하지 않는 리뷰 삭제 시 예외가 발생한다")
    void deleteReview_notFound_exception() {
        // given
        UUID reviewId = UUID.randomUUID();

        given(reviewRepository.findByIdWithAuthor(reviewId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, UUID.randomUUID()))
            .isInstanceOf(ReviewNotFoundException.class);
    }

    @Test
    @DisplayName("본인의 리뷰가 아니면 삭제 시 예외가 발생한다")
    void deleteReview_forbidden_exception() {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();

        Review review = mock(Review.class);
        given(reviewRepository.findByIdWithAuthor(reviewId)).willReturn(Optional.of(review));
        given(review.getAuthorId()).willReturn(otherId);

        // when & then
        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, authorId))
            .isInstanceOf(ReviewForbiddenException.class);
    }
}
