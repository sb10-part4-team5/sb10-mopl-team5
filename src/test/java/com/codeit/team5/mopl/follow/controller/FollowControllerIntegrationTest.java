package com.codeit.team5.mopl.follow.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.auth.dto.request.SignInRequest;
import com.codeit.team5.mopl.follow.dto.request.FollowCreateRequest;
import com.codeit.team5.mopl.follow.entity.Follow;
import com.codeit.team5.mopl.follow.repository.FollowRepository;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "jwt.access-secret-key=abcdefghijklmnopqrstuvwxyz123456",
        "jwt.refresh-secret-key=123456abcdefghijklmnopqrstuvwxyz",
        "jwt.access-token-expiration-minutes=30",
        "jwt.refresh-token-expiration-minutes=420"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class FollowControllerIntegrationTest {

    private static final String PASSWORD = "password1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User saveUser(String email, String name) {
        return userRepository.saveAndFlush(
                User.create(email, passwordEncoder.encode(PASSWORD), name));
    }

    private String tokenOf(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignInRequest(email, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    private String bearer(String email) throws Exception {
        return "Bearer " + tokenOf(email);
    }

    @Test
    @DisplayName("팔로우 성공")
    void follow_success() throws Exception {
        User follower = saveUser("follower@mopl.com", "팔로워");
        User followee = saveUser("followee@mopl.com", "팔로위");

        mockMvc.perform(post("/api/follows")
                        .header("Authorization", bearer("follower@mopl.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FollowCreateRequest(followee.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.followeeId").value(followee.getId().toString()))
                .andExpect(jsonPath("$.followerId").value(follower.getId().toString()));

        assertThat(followRepository.findByFollowerIdAndFolloweeId(follower.getId(), followee.getId()))
                .isPresent();
    }

    @Test
    @DisplayName("자기 자신 팔로우 실패")
    void follow_self_returnsBadRequest() throws Exception {
        User user = saveUser("self@mopl.com", "본인");

        mockMvc.perform(post("/api/follows")
                        .header("Authorization", bearer("self@mopl.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FollowCreateRequest(user.getId()))))
                .andExpect(status().isBadRequest());

        assertThat(followRepository.count()).isZero();
    }

    @Test
    @DisplayName("이미 팔로우한 사용자 재팔로우 실패")
    void follow_duplicate_returnsConflict() throws Exception {
        User follower = saveUser("follower@mopl.com", "팔로워");
        User followee = saveUser("followee@mopl.com", "팔로위");
        followRepository.saveAndFlush(Follow.create(follower, followee));

        mockMvc.perform(post("/api/follows")
                        .header("Authorization", bearer("follower@mopl.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FollowCreateRequest(followee.getId()))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("인증 없이 팔로우 요청 실패")
    void follow_unauthenticated_returnsUnauthorized() throws Exception {
        User followee = saveUser("followee@mopl.com", "팔로위");

        mockMvc.perform(post("/api/follows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FollowCreateRequest(followee.getId()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("팔로우 여부 확인 - 팔로우 중이면 성공")
    void getFollowedByMe_following_success() throws Exception {
        User follower = saveUser("follower@mopl.com", "팔로워");
        User followee = saveUser("followee@mopl.com", "팔로위");
        followRepository.saveAndFlush(Follow.create(follower, followee));

        mockMvc.perform(get("/api/follows/followed-by-me")
                        .header("Authorization", bearer("follower@mopl.com"))
                        .param("followeeId", followee.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followeeId").value(followee.getId().toString()));
    }

    @Test
    @DisplayName("팔로우 여부 확인 - 팔로우하지 않으면 실패")
    void getFollowedByMe_notFollowing_returnsNotFound() throws Exception {
        saveUser("follower@mopl.com", "팔로워");
        User followee = saveUser("followee@mopl.com", "팔로위");

        mockMvc.perform(get("/api/follows/followed-by-me")
                        .header("Authorization", bearer("follower@mopl.com"))
                        .param("followeeId", followee.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("팔로워 수 조회 성공")
    void countFollowers_success() throws Exception {
        User target = saveUser("target@mopl.com", "타겟");
        User follower1 = saveUser("f1@mopl.com", "팔로워1");
        User follower2 = saveUser("f2@mopl.com", "팔로워2");
        followRepository.saveAndFlush(Follow.create(follower1, target));
        followRepository.saveAndFlush(Follow.create(follower2, target));

        mockMvc.perform(get("/api/follows/count")
                        .header("Authorization", bearer("target@mopl.com"))
                        .param("followeeId", target.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string("2"));
    }

    @Test
    @DisplayName("팔로우 취소 성공")
    void unfollow_success() throws Exception {
        User follower = saveUser("follower@mopl.com", "팔로워");
        User followee = saveUser("followee@mopl.com", "팔로위");
        Follow saved = followRepository.saveAndFlush(Follow.create(follower, followee));

        mockMvc.perform(delete("/api/follows/{followId}", saved.getId())
                        .header("Authorization", bearer("follower@mopl.com")))
                .andExpect(status().isNoContent());

        assertThat(followRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("남의 팔로우는 취소 실패")
    void unfollow_otherUsersFollow_returnsForbidden() throws Exception {
        User follower = saveUser("follower@mopl.com", "팔로워");
        User followee = saveUser("followee@mopl.com", "팔로위");
        saveUser("attacker@mopl.com", "공격자");
        Follow saved = followRepository.saveAndFlush(Follow.create(follower, followee));

        // 제3자(attacker)가 follower의 팔로우를 취소 시도 → 403
        mockMvc.perform(delete("/api/follows/{followId}", saved.getId())
                        .header("Authorization", bearer("attacker@mopl.com")))
                .andExpect(status().isForbidden());

        assertThat(followRepository.findById(saved.getId())).isPresent();
    }
}
