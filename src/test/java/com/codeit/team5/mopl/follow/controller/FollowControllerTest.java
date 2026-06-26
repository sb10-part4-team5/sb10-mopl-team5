package com.codeit.team5.mopl.follow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.team5.mopl.auth.filter.JwtAuthenticationFilter;
import com.codeit.team5.mopl.auth.handler.UserAccessDeniedHandler;
import com.codeit.team5.mopl.auth.handler.UserAuthenticationEntryPoint;
import com.codeit.team5.mopl.auth.jwt.JwtTokenizer;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetailsService;
import com.codeit.team5.mopl.auth.security.provider.MoplAuthenticationProvider;
import com.codeit.team5.mopl.config.SecurityConfig;
import com.codeit.team5.mopl.follow.dto.request.FollowCreateRequest;
import com.codeit.team5.mopl.follow.dto.response.FollowResponse;
import com.codeit.team5.mopl.follow.exception.DuplicateFollowException;
import com.codeit.team5.mopl.follow.exception.FollowForbiddenException;
import com.codeit.team5.mopl.follow.exception.FollowNotFoundException;
import com.codeit.team5.mopl.follow.exception.SelfFollowException;
import com.codeit.team5.mopl.follow.service.FollowService;
import com.codeit.team5.mopl.global.exception.GlobalExceptionHandler;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import com.codeit.team5.mopl.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
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

@WebMvcTest(FollowController.class)
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        UserAuthenticationEntryPoint.class,
        UserAccessDeniedHandler.class
})
class FollowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FollowService followService;

    @MockitoBean
    private JwtTokenizer jwtTokenizer;

    @MockitoBean
    private MoplUserDetailsService userDetailsService;

    @MockitoBean
    private MoplAuthenticationProvider moplAuthenticationProvider;

    @MockitoBean
    private UserRepository userRepository;

    private Authentication authOf(UUID userId) {
        UserResponse dto = new UserResponse(
                userId, Instant.now(), "user@mopl.com", "유저", null, "USER", false);
        MoplUserDetails details = new MoplUserDetails(dto, "password");
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }

    @Test
    @DisplayName("팔로우 요청 성공")
    void follow_success() throws Exception {
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        FollowResponse response = new FollowResponse(UUID.randomUUID(), followeeId, followerId);
        given(followService.follow(eq(followerId), eq(followeeId))).willReturn(response);

        mockMvc.perform(post("/api/follows")
                        .with(authentication(authOf(followerId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FollowCreateRequest(followeeId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.followeeId").value(followeeId.toString()))
                .andExpect(jsonPath("$.followerId").value(followerId.toString()));
    }

    @Test
    @DisplayName("인증 없이 팔로우 요청하면 실패")
    void follow_unauthenticated_throwsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/follows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FollowCreateRequest(UUID.randomUUID()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("자기 자신 팔로우 요청하면 실패")
    void follow_self_throwsBadRequest() throws Exception {
        UUID userId = UUID.randomUUID();
        given(followService.follow(any(), any())).willThrow(new SelfFollowException(userId));

        mockMvc.perform(post("/api/follows")
                        .with(authentication(authOf(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FollowCreateRequest(userId))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이미 팔로우한 사용자면 실패")
    void follow_duplicate_throwsConflict() throws Exception {
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        given(followService.follow(any(), any())).willThrow(new DuplicateFollowException(followerId, followeeId));

        mockMvc.perform(post("/api/follows")
                        .with(authentication(authOf(followerId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FollowCreateRequest(followeeId))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("팔로우 여부 확인 - 팔로우하지 않으면 실패")
    void getFollowedByMe_notFollowing_throwsNotFound() throws Exception {
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        given(followService.getFollowedByMe(eq(followerId), eq(followeeId)))
                .willThrow(new FollowNotFoundException(followerId, followeeId));

        mockMvc.perform(get("/api/follows/followed-by-me")
                        .with(authentication(authOf(followerId)))
                        .param("followeeId", followeeId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("팔로워 수 조회 성공")
    void countFollowers_success() throws Exception {
        UUID followeeId = UUID.randomUUID();
        given(followService.countFollowers(followeeId)).willReturn(5L);

        mockMvc.perform(get("/api/follows/count")
                        .with(authentication(authOf(UUID.randomUUID())))
                        .param("followeeId", followeeId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    @DisplayName("팔로우 취소 성공")
    void unfollow_success() throws Exception {
        UUID requesterId = UUID.randomUUID();
        UUID followId = UUID.randomUUID();

        mockMvc.perform(delete("/api/follows/{followId}", followId)
                        .with(authentication(authOf(requesterId))))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("본인의 팔로우가 아니면 취소 실패")
    void unfollow_forbidden_throwsForbidden() throws Exception {
        UUID requesterId = UUID.randomUUID();
        UUID followId = UUID.randomUUID();
        willThrow(new FollowForbiddenException(followId, requesterId))
                .given(followService).unfollow(eq(requesterId), eq(followId));

        mockMvc.perform(delete("/api/follows/{followId}", followId)
                        .with(authentication(authOf(requesterId))))
                .andExpect(status().isForbidden());
    }
}
