package com.codeit.team5.mopl.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.auth.security.details.AuthUser;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.notification.entity.Notification;
import com.codeit.team5.mopl.notification.entity.NotificationLevel;
import com.codeit.team5.mopl.notification.entity.NotificationType;
import com.codeit.team5.mopl.notification.repository.NotificationRepository;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// 알림 컨트롤러 통합 테스트
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    private UUID persistReceiver(String email) {
        User user = User.create(email, "password", "수신자");
        return userRepository.saveAndFlush(user).getId();
    }

    private Authentication authOf(UUID userId, String email) {
        UserResponse dto = new UserResponse(
                userId, Instant.now(), email, "수신자", null, "USER", false);
        MoplUserDetails details = new MoplUserDetails(new AuthUser(dto.id(), dto.email(), dto.role(), dto.locked()), "password");
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }

    private Notification persistNotification(UUID receiverId, String title) {
        Notification notification = Notification.create(
                receiverId, NotificationType.FOLLOWED, title, "내용", NotificationLevel.INFO);
        return notificationRepository.saveAndFlush(notification);
    }

    // ===== GET /api/notifications =====

    @Test
    @DisplayName("알림 목록 조회에 성공하고 응답에 알림 데이터와 페이지 메타가 포함된다")
    void getNotifications_success() throws Exception {
        // Given
        UUID receiverId = persistReceiver("list@example.com");
        Notification saved = persistNotification(receiverId, "알림 제목");

        // When & Then
        mockMvc.perform(get("/api/notifications")
                        .with(authentication(authOf(receiverId, "list@example.com")))
                        .param("sortDirection", "DESCENDING")
                        .param("sortBy", "createdAt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.data[0].title").value("알림 제목"))
                .andExpect(jsonPath("$.data[0].content").value("내용"))
                .andExpect(jsonPath("$.data[0].level").value("INFO"))
                .andExpect(jsonPath("$.data[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.sortBy").value("createdAt"))
                .andExpect(jsonPath("$.sortDirection").value("DESCENDING"));
    }

    @Test
    @DisplayName("다른 수신자의 알림은 조회되지 않는다")
    void getNotifications_filtersByReceiver() throws Exception {
        // Given
        UUID myId = persistReceiver("me@example.com");
        UUID otherId = persistReceiver("other@example.com");
        persistNotification(myId, "내 알림");
        persistNotification(otherId, "남의 알림");

        // When & Then
        mockMvc.perform(get("/api/notifications")
                        .with(authentication(authOf(myId, "me@example.com")))
                        .param("sortDirection", "DESCENDING")
                        .param("sortBy", "createdAt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.data[0].title").value("내 알림"));
    }

    @Test
    @DisplayName("커서 페이지네이션으로 다음 페이지를 조회한다")
    void getNotifications_cursorPagination() throws Exception {
        // Given
        UUID receiverId = persistReceiver("page@example.com");
        for (int i = 0; i < 3; i++) {
            persistNotification(receiverId, "알림" + i);
        }

        // When: 첫 페이지 (limit=2)
        String firstPageResponse = mockMvc.perform(get("/api/notifications")
                        .with(authentication(authOf(receiverId, "page@example.com")))
                        .param("limit", "2")
                        .param("sortDirection", "DESCENDING")
                        .param("sortBy", "createdAt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty())
                .andExpect(jsonPath("$.nextIdAfter").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        // 두 번째 페이지: 첫 페이지에서 받은 커서 사용
        com.fasterxml.jackson.databind.JsonNode firstPage =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(firstPageResponse);
        String nextCursor = firstPage.get("nextCursor").asText();
        String nextIdAfter = firstPage.get("nextIdAfter").asText();

        // Then: 두 번째 페이지
        mockMvc.perform(get("/api/notifications")
                        .with(authentication(authOf(receiverId, "page@example.com")))
                        .param("cursor", nextCursor)
                        .param("idAfter", nextIdAfter)
                        .param("limit", "2")
                        .param("sortDirection", "DESCENDING")
                        .param("sortBy", "createdAt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @DisplayName("인증 없이 조회하면 에러를 반환한다")
    void getNotifications_unauthenticated_returnsError() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .param("sortDirection", "DESCENDING")
                        .param("sortBy", "createdAt"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isGreaterThanOrEqualTo(400));
    }

    @Test
    @DisplayName("지원하지 않는 sortBy로 조회하면 400을 반환한다")
    void getNotifications_invalidSortBy_returnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/notifications")
                        .with(authentication(authOf(UUID.randomUUID(), "sort@example.com")))
                        .param("sortBy", "title")
                        .param("sortDirection", "DESCENDING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("InvalidSortByException"));
    }

    @Test
    @DisplayName("지원하지 않는 sortDirection으로 조회하면 400을 반환한다")
    void getNotifications_invalidSortDirection_returnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/notifications")
                        .with(authentication(authOf(UUID.randomUUID(), "sort@example.com")))
                        .param("sortBy", "createdAt")
                        .param("sortDirection", "RANDOM"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("InvalidSortDirectionException"));
    }

    // ===== DELETE /api/notifications/{notificationId} =====

    @Test
    @DisplayName("알림 읽음 처리에 성공하면 204를 반환하고 DB에 반영된다")
    void readNotification_success() throws Exception {
        // Given
        UUID receiverId = persistReceiver("read@example.com");
        Notification saved = persistNotification(receiverId, "읽을 알림");

        // When & Then
        mockMvc.perform(delete("/api/notifications/{notificationId}", saved.getId())
                        .with(authentication(authOf(receiverId, "read@example.com")))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        Notification found = notificationRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.isRead()).isTrue();
        assertThat(found.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 알림 읽음 처리 시 404를 반환한다")
    void readNotification_notFound_returns404() throws Exception {
        // Given
        UUID receiverId = persistReceiver("notfound@example.com");
        UUID fakeId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(delete("/api/notifications/{notificationId}", fakeId)
                        .with(authentication(authOf(receiverId, "notfound@example.com")))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.exceptionType").value("NotificationNotFoundException"));
    }

    @Test
    @DisplayName("다른 수신자의 알림은 읽음 처리할 수 없다")
    void readNotification_otherReceiver_returns404() throws Exception {
        // Given
        UUID ownerId = persistReceiver("owner@example.com");
        UUID attackerId = persistReceiver("attacker@example.com");
        Notification saved = persistNotification(ownerId, "남의 알림");

        // When & Then
        mockMvc.perform(delete("/api/notifications/{notificationId}", saved.getId())
                        .with(authentication(authOf(attackerId, "attacker@example.com")))
                        .with(csrf()))
                .andExpect(status().isNotFound());

        Notification notRead = notificationRepository.findById(saved.getId()).orElseThrow();
        assertThat(notRead.isRead()).isFalse();
    }
}
