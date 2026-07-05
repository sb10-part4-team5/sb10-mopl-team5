package com.codeit.team5.mopl.review.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeit.team5.mopl.config.JpaAuditingConfig;
import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.global.support.config.QueryDslTestConfig;
import com.codeit.team5.mopl.review.entity.Review;
import com.codeit.team5.mopl.user.entity.User;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({JpaAuditingConfig.class, TestcontainersConfiguration.class, QueryDslTestConfig.class})
class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private EntityManager entityManager;

    private User persistUser(String email) {
        User user = User.create(email, "password", "테스터");
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    private Content persistContent(String title) {
        Content content = Content.createByAdmin(ContentType.MOVIE, title, null);
        entityManager.persist(content);
        entityManager.flush();
        return content;
    }

    private Review persistReview(Content content, User author, String text, double rating) {
        Review review = Review.of(content, author, text, rating);
        reviewRepository.save(review);
        entityManager.flush();
        return review;
    }

    private void setCreatedAt(java.util.UUID reviewId, Instant createdAt) {
        entityManager.createNativeQuery(
                "UPDATE reviews SET created_at = :ts WHERE id = :id")
            .setParameter("ts", OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC))
            .setParameter("id", reviewId)
            .executeUpdate();
    }

    @Test
    @DisplayName("리뷰 저장에 성공하고 생성 시각이 자동으로 기록된다")
    void save_success() {
        // given
        User author = persistUser("save@example.com");
        Content content = persistContent("영화1");

        // when
        Review saved = persistReview(content, author, "재밌어요", 4.5);
        entityManager.clear();

        // then
        Review found = reviewRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getContentId()).isEqualTo(content.getId());
        assertThat(found.getAuthorId()).isEqualTo(author.getId());
        assertThat(found.getText()).isEqualTo("재밌어요");
        assertThat(found.getRating()).isEqualTo(4.5);
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("동일 콘텐츠에 동일 작성자가 두 번 저장하면 예외가 발생한다")
    void save_duplicateReview_throwsException() {
        // given
        User author = persistUser("dup@example.com");
        Content content = persistContent("영화2");
        reviewRepository.saveAndFlush(Review.of(content, author, "첫 리뷰", 4.0));

        // when & then
        assertThatThrownBy(() ->
            reviewRepository.saveAndFlush(Review.of(content, author, "중복 리뷰", 3.0)))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("existsByContent_IdAndAuthor_Id: 리뷰가 존재하면 true를 반환한다")
    void existsByContent_IdAndAuthor_Id_exists() {
        // given
        User author = persistUser("exists@example.com");
        Content content = persistContent("영화3");
        persistReview(content, author, "리뷰", 4.0);
        entityManager.clear();

        // when & then
        assertThat(reviewRepository.existsByContent_IdAndAuthor_Id(content.getId(), author.getId())).isTrue();
    }

    @Test
    @DisplayName("existsByContent_IdAndAuthor_Id: 리뷰가 없으면 false를 반환한다")
    void existsByContent_IdAndAuthor_Id_notExists() {
        // given
        User author = persistUser("notexists@example.com");
        Content content = persistContent("영화4");

        // when & then
        assertThat(reviewRepository.existsByContent_IdAndAuthor_Id(content.getId(), author.getId())).isFalse();
    }

    @Test
    @DisplayName("countByContent_Id: 콘텐츠의 리뷰 수를 정확히 반환한다")
    void countByContent_Id_success() {
        // given
        User author1 = persistUser("count1@example.com");
        User author2 = persistUser("count2@example.com");
        Content content = persistContent("영화5");
        Content otherContent = persistContent("영화6");

        persistReview(content, author1, "리뷰1", 4.0);
        persistReview(content, author2, "리뷰2", 3.5);
        persistReview(otherContent, author1, "다른 콘텐츠 리뷰", 5.0);
        entityManager.clear();

        // when & then
        assertThat(reviewRepository.countByContent_Id(content.getId())).isEqualTo(2);
    }

    @Test
    @DisplayName("createdAt 내림차순 커서 페이지네이션: 두 페이지가 겹치지 않고 이어진다")
    void findPageByContentIdSortByCreatedAt_desc_pagination() {
        // given
        User author1 = persistUser("page-desc1@example.com");
        User author2 = persistUser("page-desc2@example.com");
        User author3 = persistUser("page-desc3@example.com");
        User author4 = persistUser("page-desc4@example.com");
        User author5 = persistUser("page-desc5@example.com");
        Content content = persistContent("영화7");
        Content otherContent = persistContent("영화8");

        Review r1 = persistReview(content, author1, "r1", 4.0);
        setCreatedAt(r1.getId(), Instant.parse("2026-01-01T00:01:00Z"));
        Review r2 = persistReview(content, author2, "r2", 3.5);
        setCreatedAt(r2.getId(), Instant.parse("2026-01-01T00:02:00Z"));
        Review r3 = persistReview(content, author3, "r3", 5.0);
        setCreatedAt(r3.getId(), Instant.parse("2026-01-01T00:03:00Z"));
        Review r4 = persistReview(content, author4, "r4", 2.0);
        setCreatedAt(r4.getId(), Instant.parse("2026-01-01T00:04:00Z"));
        Review r5 = persistReview(content, author5, "r5", 1.5);
        setCreatedAt(r5.getId(), Instant.parse("2026-01-01T00:05:00Z"));
        persistReview(otherContent, author1, "other", 4.0);
        entityManager.flush();
        entityManager.clear();

        // when
        List<Review> firstPage = reviewRepository.findPageByContentIdSortByCreatedAt(
            content.getId(), null, null, Limit.of(2), Sort.Direction.DESC);
        Review cursor = firstPage.get(firstPage.size() - 1);
        List<Review> secondPage = reviewRepository.findPageByContentIdSortByCreatedAt(
            content.getId(), cursor.getCreatedAt(), cursor.getId(), Limit.of(2), Sort.Direction.DESC);

        // then
        assertThat(firstPage).hasSize(2);
        assertThat(secondPage).hasSize(2);
        assertThat(secondPage).extracting(Review::getId)
            .doesNotContainAnyElementsOf(firstPage.stream().map(Review::getId).toList());
    }

    @Test
    @DisplayName("createdAt 내림차순 조회는 최신 리뷰가 먼저 반환된다")
    void findPageByContentIdSortByCreatedAt_desc_ordering() {
        // given
        User author1 = persistUser("order-desc1@example.com");
        User author2 = persistUser("order-desc2@example.com");
        User author3 = persistUser("order-desc3@example.com");
        Content content = persistContent("영화9");

        Review r1 = persistReview(content, author1, "r1", 4.0);
        setCreatedAt(r1.getId(), Instant.parse("2026-01-01T00:01:00Z"));
        Review r2 = persistReview(content, author2, "r2", 3.5);
        setCreatedAt(r2.getId(), Instant.parse("2026-01-01T00:02:00Z"));
        Review r3 = persistReview(content, author3, "r3", 5.0);
        setCreatedAt(r3.getId(), Instant.parse("2026-01-01T00:03:00Z"));
        entityManager.flush();
        entityManager.clear();

        // when
        List<Review> all = reviewRepository.findPageByContentIdSortByCreatedAt(
            content.getId(), null, null, Limit.of(10), Sort.Direction.DESC);

        // then
        assertThat(all).hasSize(3);
        assertThat(all).isSortedAccordingTo(Comparator.comparing(Review::getCreatedAt).reversed());
    }

    @Test
    @DisplayName("createdAt 오름차순 조회는 오래된 리뷰가 먼저 반환된다")
    void findPageByContentIdSortByCreatedAt_asc_ordering() {
        // given
        User author1 = persistUser("order-asc1@example.com");
        User author2 = persistUser("order-asc2@example.com");
        User author3 = persistUser("order-asc3@example.com");
        Content content = persistContent("영화10");

        Review r1 = persistReview(content, author1, "r1", 4.0);
        setCreatedAt(r1.getId(), Instant.parse("2026-01-01T00:01:00Z"));
        Review r2 = persistReview(content, author2, "r2", 3.5);
        setCreatedAt(r2.getId(), Instant.parse("2026-01-01T00:02:00Z"));
        Review r3 = persistReview(content, author3, "r3", 5.0);
        setCreatedAt(r3.getId(), Instant.parse("2026-01-01T00:03:00Z"));
        entityManager.flush();
        entityManager.clear();

        // when
        List<Review> all = reviewRepository.findPageByContentIdSortByCreatedAt(
            content.getId(), null, null, Limit.of(10), Sort.Direction.ASC);

        // then
        assertThat(all).hasSize(3);
        assertThat(all).isSortedAccordingTo(Comparator.comparing(Review::getCreatedAt));
    }

    @Test
    @DisplayName("rating 내림차순 커서 페이지네이션: 두 페이지가 겹치지 않고 이어진다")
    void findPageByContentIdSortByRating_desc_pagination() {
        // given
        User author1 = persistUser("rating-page1@example.com");
        User author2 = persistUser("rating-page2@example.com");
        User author3 = persistUser("rating-page3@example.com");
        User author4 = persistUser("rating-page4@example.com");
        Content content = persistContent("영화11");

        persistReview(content, author1, "r1", 5.0);
        persistReview(content, author2, "r2", 4.0);
        persistReview(content, author3, "r3", 3.0);
        persistReview(content, author4, "r4", 2.0);
        entityManager.flush();
        entityManager.clear();

        // when
        List<Review> firstPage = reviewRepository.findPageByContentIdSortByRating(
            content.getId(), null, null, Limit.of(2), Sort.Direction.DESC);
        Review cursor = firstPage.get(firstPage.size() - 1);
        List<Review> secondPage = reviewRepository.findPageByContentIdSortByRating(
            content.getId(), cursor.getRating(), cursor.getId(), Limit.of(2), Sort.Direction.DESC);

        // then
        assertThat(firstPage).hasSize(2);
        assertThat(secondPage).hasSize(2);
        assertThat(secondPage).extracting(Review::getId)
            .doesNotContainAnyElementsOf(firstPage.stream().map(Review::getId).toList());
    }

    @Test
    @DisplayName("rating 내림차순 조회는 높은 평점이 먼저 반환된다")
    void findPageByContentIdSortByRating_desc_ordering() {
        // given
        User author1 = persistUser("rating-order1@example.com");
        User author2 = persistUser("rating-order2@example.com");
        User author3 = persistUser("rating-order3@example.com");
        Content content = persistContent("영화12");

        persistReview(content, author1, "r1", 3.0);
        persistReview(content, author2, "r2", 5.0);
        persistReview(content, author3, "r3", 1.0);
        entityManager.flush();
        entityManager.clear();

        // when
        List<Review> all = reviewRepository.findPageByContentIdSortByRating(
            content.getId(), null, null, Limit.of(10), Sort.Direction.DESC);

        // then
        assertThat(all).hasSize(3);
        assertThat(all).isSortedAccordingTo(Comparator.comparing(Review::getRating).reversed());
    }

    @Test
    @DisplayName("rating 오름차순 조회는 낮은 평점이 먼저 반환된다")
    void findPageByContentIdSortByRating_asc_ordering() {
        // given
        User author1 = persistUser("rating-asc1@example.com");
        User author2 = persistUser("rating-asc2@example.com");
        User author3 = persistUser("rating-asc3@example.com");
        Content content = persistContent("영화13");

        persistReview(content, author1, "r1", 3.0);
        persistReview(content, author2, "r2", 5.0);
        persistReview(content, author3, "r3", 1.0);
        entityManager.flush();
        entityManager.clear();

        // when
        List<Review> all = reviewRepository.findPageByContentIdSortByRating(
            content.getId(), null, null, Limit.of(10), Sort.Direction.ASC);

        // then
        assertThat(all).hasSize(3);
        assertThat(all).isSortedAccordingTo(Comparator.comparing(Review::getRating));
    }
}
