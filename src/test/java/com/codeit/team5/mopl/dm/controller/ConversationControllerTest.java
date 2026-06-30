package com.codeit.team5.mopl.dm.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.team5.mopl.TestGlobalExceptionHandlerConfig;
import com.codeit.team5.mopl.auth.filter.JwtAuthenticationFilter;
import com.codeit.team5.mopl.auth.handler.UserAccessDeniedHandler;
import com.codeit.team5.mopl.auth.handler.UserAuthenticationEntryPoint;
import com.codeit.team5.mopl.auth.jwt.JwtTokenizer;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetailsService;
import com.codeit.team5.mopl.auth.security.provider.MoplAuthenticationProvider;
import com.codeit.team5.mopl.config.SecurityConfig;
import com.codeit.team5.mopl.dm.dto.request.ConversationCreateRequest;
import com.codeit.team5.mopl.dm.dto.response.ConversationResponse;
import com.codeit.team5.mopl.dm.service.DmService;
import com.codeit.team5.mopl.global.exception.GlobalExceptionHandler;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import com.codeit.team5.mopl.user.dto.response.UserSummaryResponse;
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

@WebMvcTest(ConversationController.class)
@Import({
        GlobalExceptionHandler.class,
        TestGlobalExceptionHandlerConfig.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        UserAuthenticationEntryPoint.class,
        UserAccessDeniedHandler.class
})
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DmService dmService;

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
    @DisplayName("대화 생성 요청 성공")
    void createConversation_success() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID withUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UserSummaryResponse with = new UserSummaryResponse(withUserId, "상대방", null);
        ConversationResponse response = new ConversationResponse(conversationId, with, null, false);
        given(dmService.getOrCreateConversation(eq(currentUserId), eq(withUserId))).willReturn(response);

        mockMvc.perform(post("/api/conversations")
                        .with(authentication(authOf(currentUserId)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConversationCreateRequest(withUserId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(conversationId.toString()))
                .andExpect(jsonPath("$.with.userId").value(withUserId.toString()))
                .andExpect(jsonPath("$.with.name").value("상대방"))
                .andExpect(jsonPath("$.hasUnread").value(false));
    }

    @Test
    @DisplayName("withUserId 누락 시 실패")
    void createConversation_missingWithUserId_throwsBadRequest() throws Exception {
        UUID currentUserId = UUID.randomUUID();

        mockMvc.perform(post("/api/conversations")
                        .with(authentication(authOf(currentUserId)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("단건 대화 조회 요청 성공")
    void getConversation_success() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID withUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UserSummaryResponse with = new UserSummaryResponse(withUserId, "상대방", null);
        ConversationResponse response = new ConversationResponse(conversationId, with, null, true);
        given(dmService.getConversation(eq(currentUserId), eq(conversationId))).willReturn(response);

        mockMvc.perform(get("/api/conversations/{conversationId}", conversationId)
                        .with(authentication(authOf(currentUserId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId.toString()))
                .andExpect(jsonPath("$.with.userId").value(withUserId.toString()))
                .andExpect(jsonPath("$.hasUnread").value(true));
    }

    @Test
    @DisplayName("상대방과의 대화 조회 요청 성공")
    void getConversationWith_success() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID withUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UserSummaryResponse with = new UserSummaryResponse(withUserId, "상대방", null);
        ConversationResponse response = new ConversationResponse(conversationId, with, null, false);
        given(dmService.getConversationWith(eq(currentUserId), eq(withUserId))).willReturn(response);

        mockMvc.perform(get("/api/conversations/with")
                        .param("userId", withUserId.toString())
                        .with(authentication(authOf(currentUserId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId.toString()))
                .andExpect(jsonPath("$.with.userId").value(withUserId.toString()))
                .andExpect(jsonPath("$.hasUnread").value(false));
    }

    @Test
    @DisplayName("메시지 읽음 처리 요청 성공")
    void markAsRead_success() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID directMessageId = UUID.randomUUID();

        mockMvc.perform(post("/api/conversations/{conversationId}/direct-messages/{directMessageId}/read",
                        conversationId, directMessageId)
                        .with(authentication(authOf(currentUserId)))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(dmService).markMessagesAsRead(eq(currentUserId), eq(conversationId), eq(directMessageId));
    }
}
