package com.codeit.team5.mopl.dm.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.team5.mopl.ControllerSliceSecurityMockConfig;
import com.codeit.team5.mopl.TestGlobalExceptionHandlerConfig;
import com.codeit.team5.mopl.auth.jwt.JwtAuthenticationFilter;
import com.codeit.team5.mopl.auth.jwt.JwtAuthenticationService;
import com.codeit.team5.mopl.auth.security.handler.UserAccessDeniedHandler;
import com.codeit.team5.mopl.auth.security.handler.UserAuthenticationEntryPoint;
import com.codeit.team5.mopl.auth.jwt.JwtTokenizer;
import com.codeit.team5.mopl.auth.security.details.AuthUser;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetailsService;
import com.codeit.team5.mopl.auth.security.handler.signin.SignInFailureHandler;
import com.codeit.team5.mopl.auth.security.handler.signin.SignInSuccessHandler;
import com.codeit.team5.mopl.auth.security.handler.signout.SignOutHandler;
import com.codeit.team5.mopl.auth.security.provider.MoplAuthenticationProvider;
import com.codeit.team5.mopl.config.SecurityConfig;
import com.codeit.team5.mopl.dm.dto.request.ConversationCreateRequest;
import com.codeit.team5.mopl.dm.dto.request.ConversationCursorRequest;
import com.codeit.team5.mopl.dm.dto.response.ConversationResponse;
import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.dm.exception.NotConversationParticipantException;
import com.codeit.team5.mopl.dm.service.ConversationService;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.global.exception.GlobalExceptionHandler;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import com.codeit.team5.mopl.user.dto.response.UserSummary;
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

@WebMvcTest(ConversationController.class)
@Import({
        GlobalExceptionHandler.class,
        TestGlobalExceptionHandlerConfig.class,
        SecurityConfig.class,
        ControllerSliceSecurityMockConfig.class,
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
    private ConversationService conversationService;

    @MockitoBean
    private JwtAuthenticationService jwtAuthenticationService;

    @MockitoBean
    private JwtTokenizer jwtTokenizer;

    @MockitoBean
    private MoplUserDetailsService userDetailsService;

    @MockitoBean
    private MoplAuthenticationProvider moplAuthenticationProvider;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private SignInSuccessHandler signInSuccessHandler;

    @MockitoBean
    private SignInFailureHandler signInFailureHandler;

    @MockitoBean
    private SignOutHandler signOutHandler;

    private Authentication authOf(UUID userId) {
        UserResponse dto = new UserResponse(
                userId, Instant.now(), "user@mopl.com", "유저", null, "USER", false);
        MoplUserDetails details = new MoplUserDetails(new AuthUser(dto.id(), dto.email(), dto.role(), dto.locked()), "password");
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }

    @Test
    @DisplayName("대화 생성 요청 성공")
    void createConversation_success() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID withUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UserSummary with = new UserSummary(withUserId, "상대방", null);
        ConversationResponse response = new ConversationResponse(conversationId, with, null, false);
        given(conversationService.getOrCreateConversation(eq(currentUserId), eq(withUserId))).willReturn(response);

        mockMvc.perform(post("/api/conversations")
                        .with(authentication(authOf(currentUserId)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConversationCreateRequest(withUserId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId.toString()))
                .andExpect(jsonPath("$.with.userId").value(withUserId.toString()))
                .andExpect(jsonPath("$.with.name").value("상대방"))
                .andExpect(jsonPath("$.hasUnread").value(false));
    }

    @Test
    @DisplayName("latestMessage가 있으면 lastestMessage JSON 키로 직렬화 성공")
    void createConversation_latestMessageSerializedAsLastestMessage_success() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID withUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UserSummary with = new UserSummary(withUserId, "상대방", null);
        DirectMessageResponse latestMsg = new DirectMessageResponse(
                UUID.randomUUID(), conversationId, with, with, "안녕", Instant.now());
        ConversationResponse response = new ConversationResponse(conversationId, with, latestMsg, false);
        given(conversationService.getOrCreateConversation(eq(currentUserId), eq(withUserId))).willReturn(response);

        mockMvc.perform(post("/api/conversations")
                        .with(authentication(authOf(currentUserId)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConversationCreateRequest(withUserId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastestMessage.content").value("안녕"));
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
        UserSummary with = new UserSummary(withUserId, "상대방", null);
        ConversationResponse response = new ConversationResponse(conversationId, with, null, true);
        given(conversationService.getConversation(eq(currentUserId), eq(conversationId))).willReturn(response);

        mockMvc.perform(get("/api/conversations/{conversationId}", conversationId)
                        .with(authentication(authOf(currentUserId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId.toString()))
                .andExpect(jsonPath("$.with.userId").value(withUserId.toString()))
                .andExpect(jsonPath("$.hasUnread").value(true));
    }

    @Test
    @DisplayName("비참여자가 단건 대화를 조회하면 실패")
    void getConversation_notParticipant_forbidden() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        given(conversationService.getConversation(eq(currentUserId), eq(conversationId)))
                .willThrow(new NotConversationParticipantException(currentUserId));

        mockMvc.perform(get("/api/conversations/{conversationId}", conversationId)
                        .with(authentication(authOf(currentUserId))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("상대방과의 대화 조회 요청 성공")
    void getConversationWith_success() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID withUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UserSummary with = new UserSummary(withUserId, "상대방", null);
        ConversationResponse response = new ConversationResponse(conversationId, with, null, false);
        given(conversationService.getConversationWith(eq(currentUserId), eq(withUserId))).willReturn(response);

        mockMvc.perform(get("/api/conversations/with")
                        .param("userId", withUserId.toString())
                        .with(authentication(authOf(currentUserId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId.toString()))
                .andExpect(jsonPath("$.with.userId").value(withUserId.toString()))
                .andExpect(jsonPath("$.hasUnread").value(false));
    }

    @Test
    @DisplayName("내 대화 목록 조회 요청 성공")
    void getMyConversations_success() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UserSummary with = new UserSummary(UUID.randomUUID(), "상대방", null);
        ConversationResponse item = new ConversationResponse(conversationId, with, null, false);
        CursorResponse<ConversationResponse> response = new CursorResponse<>(
                List.of(item), null, null, false, 1L, "createdAt", "DESCENDING");
        given(conversationService.findMyConversations(eq(currentUserId), any(ConversationCursorRequest.class)))
                .willReturn(response);

        mockMvc.perform(get("/api/conversations")
                        .param("limit", "20")
                        .param("sortDirection", "DESCENDING")
                        .param("sortBy", "createdAt")
                        .with(authentication(authOf(currentUserId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(conversationId.toString()))
                .andExpect(jsonPath("$.data[0].with.name").value("상대방"))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.sortBy").value("createdAt"))
                .andExpect(jsonPath("$.sortDirection").value("DESCENDING"));
    }

    @Test
    @DisplayName("limit 누락 시 내 대화 목록 조회 실패")
    void getMyConversations_missingLimit_throwsBadRequest() throws Exception {
        UUID currentUserId = UUID.randomUUID();

        mockMvc.perform(get("/api/conversations")
                        .param("sortDirection", "DESCENDING")
                        .with(authentication(authOf(currentUserId))))
                .andExpect(status().isBadRequest());
    }
}
