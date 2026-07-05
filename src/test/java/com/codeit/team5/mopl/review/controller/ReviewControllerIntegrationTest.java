package com.codeit.team5.mopl.review.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.auth.security.details.AuthUser;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.review.dto.request.ReviewCreateRequest;
import com.codeit.team5.mopl.review.dto.request.ReviewUpdateRequest;
import com.codeit.team5.mopl.review.entity.Review;
import com.codeit.team5.mopl.review.repository.ReviewRepository;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class ReviewControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    private User persistUser(String email) {
        return userRepository.saveAndFlush(User.create(email, "password", "테스터"));
    }

    private Content persistContent(String title) {
        return contentRepository.saveAndFlush(
            Content.createByAdmin(ContentType.MOVIE, title, null));
    }

    private Review persistReview(Content content, User author, String text, double rating) {
        return reviewRepository.saveAndFlush(Review.of(content, author, text, rating));
    }

    private Authentication authOf(UUID userId, String email) {
        MoplUserDetails details = new MoplUserDetails(
            new AuthUser(userId, email, "USER", false), "password");
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }

    // ===== GET /api/reviews =====

    @Test
    @DisplayName("리뷰 목록 조회에 성공하고 응답에 리뷰 데이터와 페이지 메타가 포함된다")
    void getReviews_success() throws Exception {
        // given
        User author = persistUser("list@example.com");
        Content content = persistContent("영화1");
        Review saved = persistReview(content, author, "재밌어요", 4.5);

        // when & then
        mockMvc.perform(get("/api/reviews")
                .with(authentication(authOf(author.getId(), "list@example.com")))
                .param("contentId", content.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value(saved.getId().toString()))
            .andExpect(jsonPath("$.data[0].contentId").value(content.getId().toString()))
            .andExpect(jsonPath("$.data[0].author.userId").value(author.getId().toString()))
            .andExpect(jsonPath("$.data[0].text").value("재밌어요"))
            .andExpect(jsonPath("$.data[0].rating").value(4.5))
            .andExpect(jsonPath("$.totalCount").value(1))
            .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @DisplayName("다른 콘텐츠의 리뷰는 조회되지 않는다")
    void getReviews_filtersByContentId() throws Exception {
        // given
        User author = persistUser("filter@example.com");
        Content myContent = persistContent("내 영화");
        Content otherContent = persistContent("다른 영화");
        persistReview(myContent, author, "내 리뷰", 4.0);
        persistReview(otherContent, author, "다른 리뷰", 3.0);

        // when & then
        mockMvc.perform(get("/api/reviews")
                .with(authentication(authOf(author.getId(), "filter@example.com")))
                .param("contentId", myContent.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalCount").value(1))
            .andExpect(jsonPath("$.data[0].text").value("내 리뷰"));
    }

    @Test
    @DisplayName("커서 페이지네이션으로 다음 페이지를 조회한다")
    void getReviews_cursorPagination() throws Exception {
        // given
        User author1 = persistUser("page1@example.com");
        User author2 = persistUser("page2@example.com");
        User author3 = persistUser("page3@example.com");
        Content content = persistContent("영화2");
        persistReview(content, author1, "리뷰1", 5.0);
        persistReview(content, author2, "리뷰2", 4.0);
        persistReview(content, author3, "리뷰3", 3.0);

        // when: 첫 페이지 (limit=2)
        String firstPageJson = mockMvc.perform(get("/api/reviews")
                .with(authentication(authOf(author1.getId(), "page1@example.com")))
                .param("contentId", content.getId().toString())
                .param("limit", "2")
                .param("sortBy", "CREATED_AT")
                .param("sortDirection", "DESC"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hasNext").value(true))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.nextCursor").isNotEmpty())
            .andExpect(jsonPath("$.nextIdAfter").isNotEmpty())
            .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.JsonNode firstPage =
            objectMapper.readTree(firstPageJson);
        String nextCursor = firstPage.get("nextCursor").asText();
        String nextIdAfter = firstPage.get("nextIdAfter").asText();

        // then: 두 번째 페이지
        mockMvc.perform(get("/api/reviews")
                .with(authentication(authOf(author1.getId(), "page1@example.com")))
                .param("contentId", content.getId().toString())
                .param("cursor", nextCursor)
                .param("idAfter", nextIdAfter)
                .param("limit", "2")
                .param("sortBy", "CREATED_AT")
                .param("sortDirection", "DESC"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @DisplayName("인증 없이 조회하면 401을 반환한다")
    void getReviews_unauthenticated_returns401() throws Exception {
        // when & then
        mockMvc.perform(get("/api/reviews")
                .param("contentId", UUID.randomUUID().toString()))
            .andExpect(status().isUnauthorized());
    }

    // ===== POST /api/reviews =====

    @Test
    @DisplayName("리뷰 생성에 성공하고 DB에 저장된다")
    void createReview_success() throws Exception {
        // given
        User author = persistUser("create@example.com");
        Content content = persistContent("영화3");
        ReviewCreateRequest request = new ReviewCreateRequest(content.getId(), "최고예요", 5.0);

        // when & then
        mockMvc.perform(post("/api/reviews")
                .with(authentication(authOf(author.getId(), "create@example.com")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.contentId").value(content.getId().toString()))
            .andExpect(jsonPath("$.author.userId").value(author.getId().toString()))
            .andExpect(jsonPath("$.text").value("최고예요"))
            .andExpect(jsonPath("$.rating").value(5.0));

        assertThat(reviewRepository.existsByContent_IdAndAuthor_Id(content.getId(), author.getId())).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 콘텐츠에 리뷰 생성하면 404를 반환한다")
    void createReview_contentNotFound_returns404() throws Exception {
        // given
        User author = persistUser("notfound@example.com");
        ReviewCreateRequest request = new ReviewCreateRequest(UUID.randomUUID(), "리뷰", 4.0);

        // when & then
        mockMvc.perform(post("/api/reviews")
                .with(authentication(authOf(author.getId(), "notfound@example.com")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.exceptionType").value("ContentNotFoundException"));
    }

    @Test
    @DisplayName("같은 콘텐츠에 두 번 리뷰 생성하면 409를 반환한다")
    void createReview_duplicate_returns409() throws Exception {
        // given
        User author = persistUser("dup@example.com");
        Content content = persistContent("영화4");
        persistReview(content, author, "첫 리뷰", 4.0);
        ReviewCreateRequest request = new ReviewCreateRequest(content.getId(), "두 번째 리뷰", 3.0);

        // when & then
        mockMvc.perform(post("/api/reviews")
                .with(authentication(authOf(author.getId(), "dup@example.com")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.exceptionType").value("ReviewAlreadyExistsException"));
    }

    // ===== PATCH /api/reviews/{reviewId} =====

    @Test
    @DisplayName("리뷰 수정에 성공하고 DB에 반영된다")
    void updateReview_success() throws Exception {
        // given
        User author = persistUser("update@example.com");
        Content content = persistContent("영화5");
        Review review = persistReview(content, author, "원래 내용", 3.0);
        ReviewUpdateRequest request = new ReviewUpdateRequest("수정된 내용", 4.0);

        // when & then
        mockMvc.perform(patch("/api/reviews/{reviewId}", review.getId())
                .with(authentication(authOf(author.getId(), "update@example.com")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.text").value("수정된 내용"))
            .andExpect(jsonPath("$.rating").value(4.0));
    }

    @Test
    @DisplayName("본인의 리뷰가 아니면 수정 시 403을 반환한다")
    void updateReview_forbidden_returns403() throws Exception {
        // given
        User owner = persistUser("owner@example.com");
        User attacker = persistUser("attacker@example.com");
        Content content = persistContent("영화6");
        Review review = persistReview(content, owner, "원래 내용", 3.0);
        ReviewUpdateRequest request = new ReviewUpdateRequest("악의적 수정", 1.0);

        // when & then
        mockMvc.perform(patch("/api/reviews/{reviewId}", review.getId())
                .with(authentication(authOf(attacker.getId(), "attacker@example.com")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.exceptionType").value("ReviewForbiddenException"));
    }

    @Test
    @DisplayName("존재하지 않는 리뷰 수정 시 404를 반환한다")
    void updateReview_notFound_returns404() throws Exception {
        // given
        User author = persistUser("upnotfound@example.com");
        ReviewUpdateRequest request = new ReviewUpdateRequest("수정 내용", 3.0);

        // when & then
        mockMvc.perform(patch("/api/reviews/{reviewId}", UUID.randomUUID())
                .with(authentication(authOf(author.getId(), "upnotfound@example.com")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.exceptionType").value("ReviewNotFoundException"));
    }

    // ===== DELETE /api/reviews/{reviewId} =====

    @Test
    @DisplayName("리뷰 삭제에 성공하고 DB에서 제거된다")
    void deleteReview_success() throws Exception {
        // given
        User author = persistUser("delete@example.com");
        Content content = persistContent("영화7");
        Review review = persistReview(content, author, "삭제할 리뷰", 3.0);

        // when & then
        mockMvc.perform(delete("/api/reviews/{reviewId}", review.getId())
                .with(authentication(authOf(author.getId(), "delete@example.com")))
                .with(csrf()))
            .andExpect(status().isNoContent());

        assertThat(reviewRepository.findById(review.getId())).isEmpty();
    }

    @Test
    @DisplayName("본인의 리뷰가 아니면 삭제 시 403을 반환한다")
    void deleteReview_forbidden_returns403() throws Exception {
        // given
        User owner = persistUser("delowner@example.com");
        User attacker = persistUser("delattacker@example.com");
        Content content = persistContent("영화8");
        Review review = persistReview(content, owner, "남의 리뷰", 4.0);

        // when & then
        mockMvc.perform(delete("/api/reviews/{reviewId}", review.getId())
                .with(authentication(authOf(attacker.getId(), "delattacker@example.com")))
                .with(csrf()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.exceptionType").value("ReviewForbiddenException"));

        assertThat(reviewRepository.findById(review.getId())).isPresent();
    }

    @Test
    @DisplayName("존재하지 않는 리뷰 삭제 시 404를 반환한다")
    void deleteReview_notFound_returns404() throws Exception {
        // given
        User author = persistUser("delnotfound@example.com");

        // when & then
        mockMvc.perform(delete("/api/reviews/{reviewId}", UUID.randomUUID())
                .with(authentication(authOf(author.getId(), "delnotfound@example.com")))
                .with(csrf()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.exceptionType").value("ReviewNotFoundException"));
    }
}
