package com.codeit.team5.mopl.dm.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
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
import com.codeit.team5.mopl.dm.dto.request.DirectMessageCursorRequest;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Sort;
import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.dm.exception.NotConversationParticipantException;
import com.codeit.team5.mopl.dm.service.DirectMessageService;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.global.exception.GlobalExceptionHandler;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DirectMessageController.class)
@Import({
        GlobalExceptionHandler.class,
        TestGlobalExceptionHandlerConfig.class,
        SecurityConfig.class,
        ControllerSliceSecurityMockConfig.class,
        JwtAuthenticationFilter.class,
        UserAuthenticationEntryPoint.class,
        UserAccessDeniedHandler.class
})
class DirectMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DirectMessageService directMessageService;

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
    @DisplayName("대화 메시지 목록 조회 요청 성공")
    void getDirectMessages_success() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        DirectMessageResponse message = new DirectMessageResponse(
                UUID.randomUUID(), conversationId, null, null, "안녕", Instant.now());
        CursorResponse<DirectMessageResponse> response = new CursorResponse<>(
                List.of(message), null, null, false, 1L, "createdAt", "DESCENDING");
        given(directMessageService.getMessages(
                eq(currentUserId), eq(conversationId), any(DirectMessageCursorRequest.class)))
                .willReturn(response);

        mockMvc.perform(get("/api/conversations/{conversationId}/direct-messages", conversationId)
                        .param("limit", "20")
                        .param("sortDirection", "DESCENDING")
                        .param("sortBy", "createdAt")
                        .with(authentication(authOf(currentUserId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("안녕"))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    @DisplayName("메시지 목록 조회 시 요청 파라미터가 서비스에 올바르게 전달 성공")
    void getDirectMessages_requestParamsPassedToService_success() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        CursorResponse<DirectMessageResponse> response = new CursorResponse<>(
                List.of(), null, null, false, 0L, "createdAt", "ASCENDING");
        given(directMessageService.getMessages(eq(currentUserId), eq(conversationId), any()))
                .willReturn(response);

        mockMvc.perform(get("/api/conversations/{conversationId}/direct-messages", conversationId)
                        .param("limit", "10")
                        .param("sortDirection", "ASCENDING")
                        .with(authentication(authOf(currentUserId))))
                .andExpect(status().isOk());

        ArgumentCaptor<DirectMessageCursorRequest> captor =
                ArgumentCaptor.forClass(DirectMessageCursorRequest.class);
        verify(directMessageService).getMessages(eq(currentUserId), eq(conversationId), captor.capture());
        assertThat(captor.getValue().limit()).isEqualTo(10);
        assertThat(captor.getValue().sortDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("비참여자가 메시지 목록 조회 시 실패")
    void getDirectMessages_notParticipant_forbidden() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        given(directMessageService.getMessages(
                eq(currentUserId), eq(conversationId), any(DirectMessageCursorRequest.class)))
                .willThrow(new NotConversationParticipantException(currentUserId));

        mockMvc.perform(get("/api/conversations/{conversationId}/direct-messages", conversationId)
                        .param("limit", "20")
                        .param("sortDirection", "DESCENDING")
                        .with(authentication(authOf(currentUserId))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("limit 누락 시 메시지 목록 조회 실패")
    void getDirectMessages_missingLimit_throwsBadRequest() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();

        mockMvc.perform(get("/api/conversations/{conversationId}/direct-messages", conversationId)
                        .param("sortDirection", "DESCENDING")
                        .with(authentication(authOf(currentUserId))))
                .andExpect(status().isBadRequest());
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

        verify(directMessageService).markMessagesAsRead(eq(currentUserId), eq(conversationId), eq(directMessageId));
    }
}
