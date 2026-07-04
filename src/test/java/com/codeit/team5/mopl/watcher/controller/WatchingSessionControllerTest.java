package com.codeit.team5.mopl.watcher.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.team5.mopl.TestGlobalExceptionHandlerConfig;
import com.codeit.team5.mopl.auth.jwt.JwtAuthenticationFilter;
import com.codeit.team5.mopl.auth.security.handler.UserAccessDeniedHandler;
import com.codeit.team5.mopl.auth.security.handler.UserAuthenticationEntryPoint;
import com.codeit.team5.mopl.auth.security.provider.MoplAuthenticationProvider;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.global.exception.GlobalExceptionHandler;
import com.codeit.team5.mopl.watcher.constant.WatcherSortByType;
import com.codeit.team5.mopl.watcher.dto.request.WatchingSessionCursorRequest;
import com.codeit.team5.mopl.watcher.dto.response.WatchingSessionResponse;
import com.codeit.team5.mopl.watcher.service.WatchingSessionService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WatchingSessionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, TestGlobalExceptionHandlerConfig.class,})
class WatchingSessionControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private WatchingSessionService service;

        @MockitoBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @MockitoBean
        private UserAuthenticationEntryPoint userAuthenticationEntryPoint;

        @MockitoBean
        private UserAccessDeniedHandler userAccessDeniedHandler;

        @MockitoBean
        private MoplAuthenticationProvider moplAuthenticationProvider;

        @Test
        @DisplayName("유저의 시청 세션 목록을 정상적으로 조회한다_성공")
        void findWatchingSessionsByWatcher_success() throws Exception {
                // Given
                UUID watcherId = UUID.randomUUID();
                UUID sessionId = UUID.randomUUID();
                WatchingSessionResponse response =
                                WatchingSessionResponse.builder().id(sessionId).build();

                given(service.findSessionByWatchId(watcherId)).willReturn(response);

                // When & Then
                mockMvc.perform(get("/api/users/{watcherId}/watching-sessions", watcherId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(sessionId.toString()));

                ArgumentCaptor<UUID> idCaptor = ArgumentCaptor.forClass(UUID.class);
                verify(service).findSessionByWatchId(idCaptor.capture());
                assertThat(idCaptor.getValue()).isEqualTo(watcherId);
        }

        @Test
        @DisplayName("유저의 시청 세션이 존재하지 않으면 빈 본문을 반환한다_성공")
        void findWatchingSessionsByWatcher_NotFound() throws Exception {
                // Given
                UUID watcherId = UUID.randomUUID();
                given(service.findSessionByWatchId(watcherId)).willReturn(null);

                // When & Then
                mockMvc.perform(get("/api/users/{watcherId}/watching-sessions", watcherId))
                                .andExpect(status().isOk()).andExpect(jsonPath("$").doesNotExist());
        }

        @Test
        @DisplayName("컨텐츠의 시청 세션 목록을 페이징하여 조회한다_성공")
        void findWatchingSessionsByContent_success() throws Exception {
                // Given
                UUID contentId = UUID.randomUUID();
                WatchingSessionResponse session =
                                WatchingSessionResponse.builder().id(UUID.randomUUID()).build();
                CursorResponse<WatchingSessionResponse> response = new CursorResponse<>(
                                List.of(session), null, null, false, 1L, "createdAt", "DESC");

                given(service.findSessionByContentId(eq(contentId),
                                any(WatchingSessionCursorRequest.class))).willReturn(response);

                // When & Then
                mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", contentId)
                                .param("limit", "10").param("sortDirection", "DESC")
                                .param("sortBy", "CREATED_AT")).andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].id").value(session.id().toString()));

                ArgumentCaptor<WatchingSessionCursorRequest> requestCaptor =
                                ArgumentCaptor.forClass(WatchingSessionCursorRequest.class);
                verify(service).findSessionByContentId(eq(contentId), requestCaptor.capture());

                WatchingSessionCursorRequest captured = requestCaptor.getValue();
                assertThat(captured.limit()).isEqualTo(10);
                assertThat(captured.sortDirection()).isEqualTo(Sort.Direction.DESC);
                assertThat(captured.sortBy()).isEqualTo(WatcherSortByType.CREATED_AT);
        }

        @ParameterizedTest(name = "limit={0}, sortDirection={1}, sortBy={2}")
        @CsvSource({", DESC, CREATED_AT", "10, , CREATED_AT", "10, DESC, "})
        @DisplayName("필수 파라미터가 누락되면 400 에러를 반환한다_실패")
        void findWatchingSessionsByContent_MissingParam(Integer limit, String sortDirection,
                        String sortBy) throws Exception {
                // Given
                UUID contentId = UUID.randomUUID();

                // When
                var requestBuilder = get("/api/contents/{contentId}/watching-sessions", contentId);
                if (limit != null) {
                        requestBuilder.param("limit", String.valueOf(limit));
                }
                if (sortDirection != null) {
                        requestBuilder.param("sortDirection", sortDirection);
                }
                if (sortBy != null) {
                        requestBuilder.param("sortBy", sortBy);
                }

                // Then
                mockMvc.perform(requestBuilder).andExpect(status().isBadRequest());
        }

        @ParameterizedTest(name = "limit={0}, sortDirection={1}, sortBy={2}")
        @CsvSource({"0, DESC, CREATED_AT", "-1, DESC, CREATED_AT"})
        @DisplayName("limit이 1 미만이면 400 에러를 반환한다_실패")
        void findWatchingSessionsByContent_InvalidLimit(Integer limit, String sortDirection,
                        String sortBy) throws Exception {
                // Given
                UUID contentId = UUID.randomUUID();

                // When
                var requestBuilder = get("/api/contents/{contentId}/watching-sessions", contentId)
                                .param("limit", String.valueOf(limit))
                                .param("sortDirection", sortDirection).param("sortBy", sortBy);

                // Then
                mockMvc.perform(requestBuilder).andExpect(status().isBadRequest());
        }
}
