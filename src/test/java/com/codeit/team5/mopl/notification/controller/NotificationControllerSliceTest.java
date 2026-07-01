package com.codeit.team5.mopl.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.team5.mopl.TestGlobalExceptionHandlerConfig;
import com.codeit.team5.mopl.auth.jwt.JwtAuthenticationFilter;
import com.codeit.team5.mopl.auth.security.handler.UserAccessDeniedHandler;
import com.codeit.team5.mopl.auth.security.handler.UserAuthenticationEntryPoint;
import com.codeit.team5.mopl.auth.jwt.JwtTokenizer;
import com.codeit.team5.mopl.auth.security.details.AuthUser;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetailsService;
import com.codeit.team5.mopl.auth.security.provider.MoplAuthenticationProvider;
import com.codeit.team5.mopl.config.SecurityConfig;
import com.codeit.team5.mopl.global.exception.GlobalExceptionHandler;
import com.codeit.team5.mopl.notification.dto.CursorResponseNotificationDto;
import com.codeit.team5.mopl.notification.dto.NotificationResponse;
import com.codeit.team5.mopl.notification.entity.NotificationLevel;
import com.codeit.team5.mopl.notification.exception.NotificationNotFoundException;
import com.codeit.team5.mopl.notification.service.NotificationService;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import com.codeit.team5.mopl.user.repository.UserRepository;
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

@WebMvcTest(NotificationController.class)
@Import({
        GlobalExceptionHandler.class,
        TestGlobalExceptionHandlerConfig.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        UserAuthenticationEntryPoint.class,
        UserAccessDeniedHandler.class
})
class NotificationControllerSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

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
        MoplUserDetails details = new MoplUserDetails(new AuthUser(dto.id(), dto.email(), dto.role(), dto.locked()), "password");
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }

    // ===== GET /api/notifications =====

    @Test
    @DisplayName("알림 목록 조회에 성공하면 200과 커서 응답을 반환한다")
    void getNotifications_success() throws Exception {
        // Given
        UUID receiverId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-06-28T00:00:00Z");

        NotificationResponse notification = new NotificationResponse(
                notificationId, createdAt, receiverId, "제목", "내용", NotificationLevel.INFO);
        CursorResponseNotificationDto response = new CursorResponseNotificationDto(
                List.of(notification), createdAt.toString(), notificationId,
                true, 5L, "createdAt", "DESCENDING");

        given(notificationService.getNotifications(
                eq(receiverId), eq(null), eq(null), eq(20),
                eq("DESCENDING"), eq("createdAt")))
                .willReturn(response);

        // When & Then
        mockMvc.perform(get("/api/notifications")
                        .with(authentication(authOf(receiverId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(notificationId.toString()))
                .andExpect(jsonPath("$.data[0].title").value("제목"))
                .andExpect(jsonPath("$.data[0].content").value("내용"))
                .andExpect(jsonPath("$.data[0].level").value("INFO"))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.totalCount").value(5))
                .andExpect(jsonPath("$.nextCursor").value(createdAt.toString()))
                .andExpect(jsonPath("$.nextIdAfter").value(notificationId.toString()))
                .andExpect(jsonPath("$.sortBy").value("createdAt"))
                .andExpect(jsonPath("$.sortDirection").value("DESCENDING"));
    }

    @Test
    @DisplayName("커서와 idAfter를 지정하면 서비스에 전달되고 200을 반환한다")
    void getNotifications_withCursor_success() throws Exception {
        // Given
        UUID receiverId = UUID.randomUUID();
        String cursor = "2026-06-28T00:00:00Z";
        UUID idAfter = UUID.randomUUID();

        CursorResponseNotificationDto response = new CursorResponseNotificationDto(
                List.of(), null, null, false, 0L, "createdAt", "ASCENDING");

        given(notificationService.getNotifications(
                eq(receiverId), eq(cursor), eq(idAfter), eq(10),
                eq("ASCENDING"), eq("createdAt")))
                .willReturn(response);

        // When & Then
        mockMvc.perform(get("/api/notifications")
                        .with(authentication(authOf(receiverId)))
                        .param("cursor", cursor)
                        .param("idAfter", idAfter.toString())
                        .param("limit", "10")
                        .param("sortDirection", "ASCENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("인증 없이 알림 목록 조회하면 401을 반환한다")
    void getNotifications_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());

        verify(notificationService, never())
                .getNotifications(any(), any(), any(), any(int.class), any(), any());
    }

    @Test
    @DisplayName("limit이 0 이하일 시 400을 반환한다.")
    void getNotifications_limit_lte_0() throws Exception{
        // Given
        UUID receiverId = UUID.randomUUID();
        String cursor = "2026-06-28T00:00:00Z";
        UUID idAfter = UUID.randomUUID();

        // When & Then
        mockMvc.perform(get("/api/notifications")
                .with(authentication(authOf(receiverId)))
                .param("cursor", cursor)
                .param("idAfter", idAfter.toString())
                .param("limit", "0")
                .param("sortDirection", "ASCENDING"))
            .andExpect(status().isBadRequest());

        verify(notificationService, never())
            .getNotifications(any(), any(), any(), any(int.class), any(), any());
    }

    @Test
    @DisplayName("limit이 100 초과일 시 400을 반환한다.")
    void getNotifications_limit_gt_100() throws Exception{
        // Given
        UUID receiverId = UUID.randomUUID();
        String cursor = "2026-06-28T00:00:00Z";
        UUID idAfter = UUID.randomUUID();

        // When & Then
        mockMvc.perform(get("/api/notifications")
                .with(authentication(authOf(receiverId)))
                .param("cursor", cursor)
                .param("idAfter", idAfter.toString())
                .param("limit", "101")
                .param("sortDirection", "ASCENDING"))
            .andExpect(status().isBadRequest());

        verify(notificationService, never())
            .getNotifications(any(), any(), any(), any(int.class), any(), any());
    }

    // ===== DELETE /api/notifications/{notificationId} =====

    @Test
    @DisplayName("알림 읽음 처리에 성공하면 204를 반환한다")
    void readNotification_success() throws Exception {
        // Given
        UUID receiverId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(delete("/api/notifications/{notificationId}", notificationId)
                        .with(authentication(authOf(receiverId)))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(notificationService).markAsRead(eq(notificationId), eq(receiverId));
    }

    @Test
    @DisplayName("존재하지 않는 알림 읽음 처리 시 404를 반환한다")
    void readNotification_notFound_returns404() throws Exception {
        // Given
        UUID receiverId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();

        given(notificationService.markAsRead(eq(notificationId), eq(receiverId)))
                .willThrow(new NotificationNotFoundException(notificationId));

        // When & Then
        mockMvc.perform(delete("/api/notifications/{notificationId}", notificationId)
                        .with(authentication(authOf(receiverId)))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.exceptionType").value("NotificationNotFoundException"));
    }

    @Test
    @DisplayName("인증 없이 읽음 처리하면 401을 반환한다")
    void readNotification_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/notifications/{notificationId}", UUID.randomUUID())
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        verify(notificationService, never()).markAsRead(any(), any());
    }
}
