package com.codeit.team5.mopl.dm.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.auth.security.details.AuthUser;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.dm.dto.request.ConversationCreateRequest;
import com.codeit.team5.mopl.dm.entity.Conversation;
import com.codeit.team5.mopl.dm.entity.DirectMessage;
import com.codeit.team5.mopl.dm.repository.ConversationRepository;
import com.codeit.team5.mopl.dm.repository.DirectMessageRepository;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class ConversationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private DirectMessageRepository directMessageRepository;

    private User saveUser(String email, String name) {
        return userRepository.saveAndFlush(User.create(email, "password", name));
    }

    private Authentication authOf(UUID userId, String email) {
        UserResponse dto = new UserResponse(
                userId, Instant.now(), email, "유저", null, "USER", false);
        MoplUserDetails details = new MoplUserDetails(new AuthUser(dto.id(), dto.email(), dto.role(), dto.locked()), "password");
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }

    @Test
    @DisplayName("대화 생성-조회-읽음 처리 전체 흐름 성공")
    void conversationFullFlow_success() throws Exception {
        User me = saveUser("me@mopl.com", "나");
        User other = saveUser("other@mopl.com", "상대");
        Authentication auth = authOf(me.getId(), "me@mopl.com");

        // 대화 생성 (getOrCreate) -> 200
        MvcResult created = mockMvc.perform(post("/api/conversations")
                        .with(csrf())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConversationCreateRequest(other.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.with.userId").value(other.getId().toString()))
                .andReturn();

        JsonNode node = objectMapper.readTree(created.getResponse().getContentAsString());
        UUID conversationId = UUID.fromString(node.get("id").asText());

        // 같은 상대로 다시 요청하면 동일한 대화 id (getOrCreate 멱등)
        mockMvc.perform(post("/api/conversations")
                        .with(csrf())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConversationCreateRequest(other.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId.toString()));

        assertThat(conversationRepository.count()).isEqualTo(1);

        // 단건 조회
        mockMvc.perform(get("/api/conversations/{conversationId}", conversationId)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId.toString()))
                .andExpect(jsonPath("$.with.userId").value(other.getId().toString()));

        // 상대 기준 조회
        mockMvc.perform(get("/api/conversations/with")
                        .with(authentication(auth))
                        .param("userId", other.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId.toString()));

        // 상대가 보낸 메시지 저장 (receiver = me)
        Conversation conversation = conversationRepository.findById(conversationId).orElseThrow();
        DirectMessage message = directMessageRepository.saveAndFlush(
                DirectMessage.create(conversation, other, "안녕"));

        // 읽음 처리
        mockMvc.perform(post("/api/conversations/{conversationId}/direct-messages/{directMessageId}/read",
                        conversationId, message.getId())
                        .with(csrf())
                        .with(authentication(auth)))
                .andExpect(status().isOk());

        DirectMessage reloaded = directMessageRepository.findById(message.getId()).orElseThrow();
        assertThat(reloaded.isRead()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 상대와의 대화 조회 실패")
    void getConversationWith_notExists_returnsNotFound() throws Exception {
        User me = saveUser("me@mopl.com", "나");
        User stranger = saveUser("stranger@mopl.com", "낯선이");

        mockMvc.perform(get("/api/conversations/with")
                        .with(authentication(authOf(me.getId(), "me@mopl.com")))
                        .param("userId", stranger.getId().toString()))
                .andExpect(status().isNotFound());
    }
}
