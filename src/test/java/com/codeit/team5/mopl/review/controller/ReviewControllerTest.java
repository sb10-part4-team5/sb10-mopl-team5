package com.codeit.team5.mopl.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.team5.mopl.TestGlobalExceptionHandlerConfig;
import com.codeit.team5.mopl.auth.jwt.JwtAuthenticationFilter;
import com.codeit.team5.mopl.auth.jwt.JwtAuthenticationService;
import com.codeit.team5.mopl.auth.jwt.JwtTokenizer;
import com.codeit.team5.mopl.auth.security.details.AuthUser;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetailsService;
import com.codeit.team5.mopl.auth.security.handler.UserAccessDeniedHandler;
import com.codeit.team5.mopl.auth.security.handler.UserAuthenticationEntryPoint;
import com.codeit.team5.mopl.auth.security.handler.signin.SignInFailureHandler;
import com.codeit.team5.mopl.auth.security.handler.signin.SignInSuccessHandler;
import com.codeit.team5.mopl.auth.security.handler.signout.SignOutHandler;
import com.codeit.team5.mopl.auth.security.provider.MoplAuthenticationProvider;
import com.codeit.team5.mopl.config.SecurityConfig;
import com.codeit.team5.mopl.content.exception.ContentNotFoundException;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.global.exception.GlobalExceptionHandler;
import com.codeit.team5.mopl.review.dto.request.ReviewCreateRequest;
import com.codeit.team5.mopl.review.dto.request.ReviewUpdateRequest;
import com.codeit.team5.mopl.review.dto.response.ReviewResponse;
import com.codeit.team5.mopl.review.exception.ReviewAlreadyExistsException;
import com.codeit.team5.mopl.review.exception.ReviewForbiddenException;
import com.codeit.team5.mopl.review.exception.ReviewNotFoundException;
import com.codeit.team5.mopl.review.service.ReviewService;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import com.codeit.team5.mopl.user.dto.response.UserSummaryResponse;
import com.codeit.team5.mopl.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReviewController.class)
@Import({
    GlobalExceptionHandler.class,
    TestGlobalExceptionHandlerConfig.class,
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    UserAuthenticationEntryPoint.class,
    UserAccessDeniedHandler.class
})
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private JwtTokenizer jwtTokenizer;

    @MockitoBean
    private JwtAuthenticationService jwtAuthenticationService;

    @MockitoBean
    private MoplUserDetailsService userDetailsService;

    @MockitoBean
    private MoplAuthenticationProvider moplAuthenticationProvider;

    @MockitoBean
    private SignInSuccessHandler signInSuccessHandler;

    @MockitoBean
    private SignInFailureHandler signInFailureHandler;

    @MockitoBean
    private SignOutHandler signOutHandler;

    @MockitoBean
    private UserRepository userRepository;

    private Authentication authOf(UUID userId) {
        UserResponse dto = new UserResponse(
            userId, Instant.now(), "user@mopl.com", "유저", null, "USER", false);
        MoplUserDetails details = new MoplUserDetails(
            new AuthUser(dto.id(), dto.email(), dto.role(), dto.locked()), "password");
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }

    private ReviewResponse sampleReviewResponse(UUID reviewId, UUID contentId, UUID authorId) {
        UserSummaryResponse author = new UserSummaryResponse(authorId, "유저", null);
        return new ReviewResponse(reviewId, contentId, author, "재밌어요", 4.5);
    }

    // ===== GET /api/reviews =====

    @Test
    @DisplayName("리뷰 목록 조회에 성공하면 200과 커서 응답을 반환한다")
    void getReviews_success() throws Exception {
        // given
        UUID contentId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        ReviewResponse review = sampleReviewResponse(reviewId, contentId, authorId);
        CursorResponse<ReviewResponse> response = new CursorResponse<>(
            List.of(review), null, null, false, 1L, "createdAt", "DESCENDING");

        given(reviewService.getReviews(
            eq(contentId), eq(null), eq(null), eq(20), eq("DESCENDING"), eq("createdAt")))
            .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/reviews")
                .with(authentication(authOf(UUID.randomUUID())))
                .param("contentId", contentId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value(reviewId.toString()))
            .andExpect(jsonPath("$.data[0].contentId").value(contentId.toString()))
            .andExpect(jsonPath("$.data[0].author.userId").value(authorId.toString()))
            .andExpect(jsonPath("$.data[0].text").value("재밌어요"))
            .andExpect(jsonPath("$.data[0].rating").value(4.5))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.totalCount").value(1))
            .andExpect(jsonPath("$.sortBy").value("createdAt"))
            .andExpect(jsonPath("$.sortDirection").value("DESCENDING"));
    }

    @Test
    @DisplayName("커서와 정렬 옵션을 지정하면 서비스에 전달되고 200을 반환한다")
    void getReviews_withCursorAndSort_success() throws Exception {
        // given
        UUID contentId = UUID.randomUUID();
        String cursor = "2026-01-01T00:00:00Z";
        UUID idAfter = UUID.randomUUID();

        CursorResponse<ReviewResponse> response = new CursorResponse<>(
            List.of(), null, null, false, 0L, "rating", "ASCENDING");

        given(reviewService.getReviews(
            eq(contentId), eq(cursor), eq(idAfter), eq(10), eq("ASCENDING"), eq("rating")))
            .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/reviews")
                .with(authentication(authOf(UUID.randomUUID())))
                .param("contentId", contentId.toString())
                .param("cursor", cursor)
                .param("idAfter", idAfter.toString())
                .param("limit", "10")
                .param("sortDirection", "ASCENDING")
                .param("sortBy", "rating"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("limit이 0이면 400을 반환한다")
    void getReviews_limitZero_returns400() throws Exception {
        // when & then
        mockMvc.perform(get("/api/reviews")
                .with(authentication(authOf(UUID.randomUUID())))
                .param("contentId", UUID.randomUUID().toString())
                .param("limit", "0"))
            .andExpect(status().isBadRequest());

        verify(reviewService, never()).getReviews(any(), any(), any(), any(int.class), any(), any());
    }

    @Test
    @DisplayName("limit이 100 초과이면 400을 반환한다")
    void getReviews_limitOver100_returns400() throws Exception {
        // when & then
        mockMvc.perform(get("/api/reviews")
                .with(authentication(authOf(UUID.randomUUID())))
                .param("contentId", UUID.randomUUID().toString())
                .param("limit", "101"))
            .andExpect(status().isBadRequest());

        verify(reviewService, never()).getReviews(any(), any(), any(), any(int.class), any(), any());
    }

    // ===== POST /api/reviews =====

    @Test
    @DisplayName("리뷰 생성에 성공하면 201과 생성된 리뷰를 반환한다")
    void ofReview_success() throws Exception {
        // given
        UUID authorId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();

        ReviewCreateRequest request = new ReviewCreateRequest(contentId, "재밌어요", 4.5);
        ReviewResponse response = sampleReviewResponse(reviewId, contentId, authorId);

        given(reviewService.createReview(eq(authorId), any(ReviewCreateRequest.class)))
            .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/reviews")
                .with(authentication(authOf(authorId)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(reviewId.toString()))
            .andExpect(jsonPath("$.contentId").value(contentId.toString()))
            .andExpect(jsonPath("$.text").value("재밌어요"))
            .andExpect(jsonPath("$.rating").value(4.5));
    }

    @Test
    @DisplayName("인증 없이 리뷰 생성하면 401을 반환한다")
    void ofReview_unauthenticated_returns401() throws Exception {
        // given
        ReviewCreateRequest request = new ReviewCreateRequest(UUID.randomUUID(), "재밌어요", 4.5);

        // when & then
        mockMvc.perform(post("/api/reviews")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());

        verify(reviewService, never()).createReview(any(), any());
    }

    @Test
    @DisplayName("contentId가 없으면 400을 반환한다")
    void ofReview_missingContentId_returns400() throws Exception {
        // given
        String body = "{\"text\":\"재밌어요\",\"rating\":4.5}";

        // when & then
        mockMvc.perform(post("/api/reviews")
                .with(authentication(authOf(UUID.randomUUID())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("평점이 5.0 초과이면 400을 반환한다")
    void ofReview_ratingOver5_returns400() throws Exception {
        // given
        ReviewCreateRequest request = new ReviewCreateRequest(UUID.randomUUID(), "재밌어요", 5.1);

        // when & then
        mockMvc.perform(post("/api/reviews")
                .with(authentication(authOf(UUID.randomUUID())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("평점이 0.0 미만이면 400을 반환한다")
    void ofReview_ratingUnder0_returns400() throws Exception {
        // given
        ReviewCreateRequest request = new ReviewCreateRequest(UUID.randomUUID(), "재밌어요", -0.1);

        // when & then
        mockMvc.perform(post("/api/reviews")
                .with(authentication(authOf(UUID.randomUUID())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이미 리뷰를 작성했으면 409를 반환한다")
    void ofReview_alreadyExists_returns409() throws Exception {
        // given
        UUID authorId = UUID.randomUUID();
        ReviewCreateRequest request = new ReviewCreateRequest(UUID.randomUUID(), "재밌어요", 4.5);

        given(reviewService.createReview(eq(authorId), any())).willThrow(new ReviewAlreadyExistsException());

        // when & then
        mockMvc.perform(post("/api/reviews")
                .with(authentication(authOf(authorId)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.exceptionType").value("ReviewAlreadyExistsException"));
    }

    @Test
    @DisplayName("존재하지 않는 콘텐츠에 리뷰 생성하면 404를 반환한다")
    void ofReview_contentNotFound_returns404() throws Exception {
        // given
        UUID authorId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        ReviewCreateRequest request = new ReviewCreateRequest(contentId, "재밌어요", 4.5);

        given(reviewService.createReview(eq(authorId), any())).willThrow(new ContentNotFoundException(contentId));

        // when & then
        mockMvc.perform(post("/api/reviews")
                .with(authentication(authOf(authorId)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.exceptionType").value("ContentNotFoundException"));
    }

    // ===== PATCH /api/reviews/{reviewId} =====

    @Test
    @DisplayName("리뷰 수정에 성공하면 200과 수정된 리뷰를 반환한다")
    void updateReview_success() throws Exception {
        // given
        UUID authorId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();

        ReviewUpdateRequest request = new ReviewUpdateRequest("수정된 내용", 3.0);
        ReviewResponse response = new ReviewResponse(reviewId, contentId,
            new UserSummaryResponse(authorId, "유저", null), "수정된 내용", 3.0);

        given(reviewService.updateReview(eq(reviewId), eq(authorId), any(ReviewUpdateRequest.class)))
            .willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/reviews/{reviewId}", reviewId)
                .with(authentication(authOf(authorId)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(reviewId.toString()))
            .andExpect(jsonPath("$.text").value("수정된 내용"))
            .andExpect(jsonPath("$.rating").value(3.0));
    }

    @Test
    @DisplayName("인증 없이 리뷰 수정하면 401을 반환한다")
    void updateReview_unauthenticated_returns401() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/reviews/{reviewId}", UUID.randomUUID())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ReviewUpdateRequest("내용", 3.0))))
            .andExpect(status().isUnauthorized());

        verify(reviewService, never()).updateReview(any(), any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 리뷰 수정 시 404를 반환한다")
    void updateReview_notFound_returns404() throws Exception {
        // given
        UUID authorId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();

        given(reviewService.updateReview(eq(reviewId), eq(authorId), any()))
            .willThrow(new ReviewNotFoundException(reviewId));

        // when & then
        mockMvc.perform(patch("/api/reviews/{reviewId}", reviewId)
                .with(authentication(authOf(authorId)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ReviewUpdateRequest("수정 내용", 3.0))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.exceptionType").value("ReviewNotFoundException"));
    }

    @Test
    @DisplayName("본인의 리뷰가 아니면 수정 시 403을 반환한다")
    void updateReview_forbidden_returns403() throws Exception {
        // given
        UUID authorId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();

        given(reviewService.updateReview(eq(reviewId), eq(authorId), any()))
            .willThrow(new ReviewForbiddenException());

        // when & then
        mockMvc.perform(patch("/api/reviews/{reviewId}", reviewId)
                .with(authentication(authOf(authorId)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ReviewUpdateRequest("수정 내용", 3.0))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.exceptionType").value("ReviewForbiddenException"));
    }

    @Test
    @DisplayName("수정 요청에서 평점이 5.0 초과이면 400을 반환한다")
    void updateReview_ratingOver5_returns400() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/reviews/{reviewId}", UUID.randomUUID())
                .with(authentication(authOf(UUID.randomUUID())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ReviewUpdateRequest("내용", 5.1))))
            .andExpect(status().isBadRequest());
    }

    // ===== DELETE /api/reviews/{reviewId} =====

    @Test
    @DisplayName("리뷰 삭제에 성공하면 204를 반환한다")
    void deleteReview_success() throws Exception {
        // given
        UUID authorId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();

        // when & then
        mockMvc.perform(delete("/api/reviews/{reviewId}", reviewId)
                .with(authentication(authOf(authorId)))
                .with(csrf()))
            .andExpect(status().isNoContent());

        verify(reviewService).deleteReview(eq(reviewId), eq(authorId));
    }

    @Test
    @DisplayName("인증 없이 리뷰 삭제하면 401을 반환한다")
    void deleteReview_unauthenticated_returns401() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/reviews/{reviewId}", UUID.randomUUID())
                .with(csrf()))
            .andExpect(status().isUnauthorized());

        verify(reviewService, never()).deleteReview(any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 리뷰 삭제 시 404를 반환한다")
    void deleteReview_notFound_returns404() throws Exception {
        // given
        UUID authorId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();

        willThrow(new ReviewNotFoundException(reviewId))
            .given(reviewService).deleteReview(eq(reviewId), eq(authorId));

        // when & then
        mockMvc.perform(delete("/api/reviews/{reviewId}", reviewId)
                .with(authentication(authOf(authorId)))
                .with(csrf()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.exceptionType").value("ReviewNotFoundException"));
    }

    @Test
    @DisplayName("본인의 리뷰가 아니면 삭제 시 403을 반환한다")
    void deleteReview_forbidden_returns403() throws Exception {
        // given
        UUID authorId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();

        willThrow(new ReviewForbiddenException())
            .given(reviewService).deleteReview(eq(reviewId), eq(authorId));

        // when & then
        mockMvc.perform(delete("/api/reviews/{reviewId}", reviewId)
                .with(authentication(authOf(authorId)))
                .with(csrf()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.exceptionType").value("ReviewForbiddenException"));
    }
}
