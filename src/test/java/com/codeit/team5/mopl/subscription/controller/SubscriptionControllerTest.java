package com.codeit.team5.mopl.subscription.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.codeit.team5.mopl.TestGlobalExceptionHandlerConfig;
import com.codeit.team5.mopl.auth.jwt.JwtAuthenticationFilter;
import com.codeit.team5.mopl.auth.security.details.MoplPrincipal;
import com.codeit.team5.mopl.global.exception.GlobalExceptionHandler;
import com.codeit.team5.mopl.subscription.exception.SubscriptionAlreadyExistsException;
import com.codeit.team5.mopl.subscription.exception.SubscriptionNotFoundException;
import com.codeit.team5.mopl.subscription.exception.SubscriptionPlaylistNotFoundException;
import com.codeit.team5.mopl.subscription.service.SubscriptionService;

@WebMvcTest(controllers = SubscriptionController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simple unit test
@Import({GlobalExceptionHandler.class, TestGlobalExceptionHandlerConfig.class})
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubscriptionService subscriptionService;

    // To mock the Principal for addFilters=false
    private final UUID testUserId = UUID.randomUUID();
    private final MoplPrincipal mockPrincipal = new MoplPrincipal() {
        @Override
        public UUID getId() {
            return testUserId;
        }

        @Override
        public String getEmail() {
            return "test@example.com";
        }

        @Override
        public String getRole() {
            return "ROLE_USER";
        }

        @Override
        public boolean isLocked() {
            return false;
        }
    };

    @BeforeEach
    void setUpSecurity() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(mockPrincipal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("플레이리스트 구독 성공")
    void createSubscription_success() throws Exception {
        UUID playlistId = UUID.randomUUID();

        doNothing().when(subscriptionService).create(eq(playlistId), eq(testUserId));

        mockMvc.perform(post("/api/playlists/{playlistId}/subscription", playlistId)).andDo(print())
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("플레이리스트 구독 실패 - 이미 구독 중")
    void createSubscription_fail_alreadyExists() throws Exception {
        UUID playlistId = UUID.randomUUID();

        doThrow(new SubscriptionAlreadyExistsException(testUserId, playlistId))
                .when(subscriptionService).create(eq(playlistId), eq(testUserId));

        mockMvc.perform(post("/api/playlists/{playlistId}/subscription", playlistId)).andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("SubscriptionAlreadyExistsException"));
    }

    @Test
    @DisplayName("플레이리스트 구독 실패 - 플레이리스트를 찾을 수 없음")
    void createSubscription_fail_playlistNotFound() throws Exception {
        UUID playlistId = UUID.randomUUID();

        doThrow(new SubscriptionPlaylistNotFoundException(playlistId)).when(subscriptionService)
                .create(eq(playlistId), eq(testUserId));

        mockMvc.perform(post("/api/playlists/{playlistId}/subscription", playlistId)).andDo(print())
                .andExpect(status().isNotFound()).andExpect(
                        jsonPath("$.exceptionType").value("SubscriptionPlaylistNotFoundException"));
    }

    @Test
    @DisplayName("플레이리스트 구독 취소 성공")
    void deleteSubscription_success() throws Exception {
        UUID playlistId = UUID.randomUUID();

        doNothing().when(subscriptionService).delete(eq(playlistId), eq(testUserId));

        mockMvc.perform(delete("/api/playlists/{playlistId}/subscription", playlistId))
                .andDo(print()).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("플레이리스트 구독 취소 실패 - 구독 내역이 없음")
    void deleteSubscription_fail_notFound() throws Exception {
        UUID playlistId = UUID.randomUUID();

        doThrow(new SubscriptionNotFoundException(playlistId, testUserId)).when(subscriptionService)
                .delete(eq(playlistId), eq(testUserId));

        mockMvc.perform(delete("/api/playlists/{playlistId}/subscription", playlistId))
                .andDo(print()).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.exceptionType").value("SubscriptionNotFoundException"));
    }
}
