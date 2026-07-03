package com.codeit.team5.mopl.playlist.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.codeit.team5.mopl.TestGlobalExceptionHandlerConfig;
import com.codeit.team5.mopl.auth.jwt.JwtAuthenticationFilter;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.global.exception.GlobalExceptionHandler;
import com.codeit.team5.mopl.playlist.dto.PlaylistCursorCommand;
import com.codeit.team5.mopl.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.team5.mopl.playlist.dto.request.PlaylistCursorRequest;
import com.codeit.team5.mopl.playlist.dto.request.PlaylistUpdateRequest;
import com.codeit.team5.mopl.playlist.dto.response.PlaylistResponse;
import com.codeit.team5.mopl.playlist.exception.PlaylistAccessDeniedException;
import com.codeit.team5.mopl.playlist.exception.PlaylistContentNotFoundException;
import com.codeit.team5.mopl.playlist.exception.PlaylistIncorrectSortByException;
import com.codeit.team5.mopl.playlist.exception.PlaylistItemNotFoundException;
import com.codeit.team5.mopl.playlist.exception.PlaylistNotFoundException;
import com.codeit.team5.mopl.playlist.exception.PlaylistSortByMismatchException;
import com.codeit.team5.mopl.playlist.mapper.PlaylistMapper;
import com.codeit.team5.mopl.playlist.service.PlaylistService;
import com.codeit.team5.mopl.user.dto.response.UserSummary;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = PlaylistController.class,
                excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, TestGlobalExceptionHandlerConfig.class})
class PlaylistControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private PlaylistService playlistService;

        @MockitoBean
        private PlaylistMapper playlistMapper;

        @Captor
        private ArgumentCaptor<PlaylistCreateRequest> createRequestCaptor;

        @Captor
        private ArgumentCaptor<PlaylistUpdateRequest> updateRequestCaptor;

        @Captor
        private ArgumentCaptor<PlaylistCursorRequest> cursorRequestCaptor;

        private Principal principal;
        private UUID playlistId;
        private UUID contentId;
        private PlaylistResponse playlistResponse;

        @BeforeEach
        void setUp() {
                principal = Mockito.mock(Principal.class);
                given(principal.getName()).willReturn("user@test.com");

                playlistId = UUID.randomUUID();
                contentId = UUID.randomUUID();

                UserSummary userSummary = new UserSummary(UUID.randomUUID(), "Test User", null);

                playlistResponse = new PlaylistResponse(playlistId, userSummary, Instant.now(),
                                "My Playlist", "Description", 0, false, List.of());
        }

        @Test
        @DisplayName("단건 조회")
        @WithMockUser
        void find() throws Exception {
                // given
                given(playlistService.find(playlistId)).willReturn(playlistResponse);

                // when & then
                mockMvc.perform(get("/api/playlists/{id}", playlistId)).andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(playlistId.toString()))
                                .andExpect(jsonPath("$.title").value("My Playlist"));
        }

        @Test
        @DisplayName("단건 조회 - 실패 (플레이리스트 없음)")
        @WithMockUser
        void find_fail_PlaylistNotFoundException() throws Exception {
                // given
                given(playlistService.find(playlistId))
                                .willThrow(new PlaylistNotFoundException(playlistId));

                // when & then
                mockMvc.perform(get("/api/playlists/{id}", playlistId)).andDo(print())
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.exceptionType")
                                                .value("PlaylistNotFoundException"))
                                .andExpect(jsonPath("$.message").value("플레이리스트를 찾을 수 없습니다."));
        }

        @Test
        @DisplayName("커서 기반 조회")
        @WithMockUser
        void findCursor() throws Exception {
                // given
                CursorResponse<PlaylistResponse> cursorResponse = CursorResponse
                                .<PlaylistResponse>builder().data(List.of(playlistResponse))
                                .hasNext(false).totalCount(1L).build();
                PlaylistCursorCommand command = PlaylistCursorCommand.builder().limit(10).build();

                given(playlistMapper.toCommand(any(PlaylistCursorRequest.class)))
                                .willReturn(command);
                given(playlistService.findByCursor(command)).willReturn(cursorResponse);

                // when & then
                mockMvc.perform(get("/api/playlists").param("limit", "10")
                                .param("sortBy", "UPDATED_AT").param("sortDirection", "DESC"))
                                .andDo(print()).andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].id").value(playlistId.toString()));

                verify(playlistMapper).toCommand(cursorRequestCaptor.capture());
                PlaylistCursorRequest captured = cursorRequestCaptor.getValue();
                assertThat(captured.limit()).isEqualTo(10);
                assertThat(captured.sortBy()).isEqualTo("UPDATED_AT");
                assertThat(captured.sortDirection()).isEqualTo(Sort.Direction.DESC);
        }

        @ParameterizedTest
        @CsvSource(value = {
                        // limit 검증
                        "null, DESC, UPDATED_AT, limit", "-5, DESC, UPDATED_AT, limit",
                        "0, DESC, UPDATED_AT, limit", "not_a_number, DESC, UPDATED_AT, limit",
                        // sortDirection 검증
                        "10, null, UPDATED_AT, sortDirection",
                        "10, INVALID, UPDATED_AT, sortDirection",
                        // sortBy 검증
                        "10, DESC, null, sortBy"}, nullValues = {"null"})
        @DisplayName("커서 기반 조회 - 실패 (입력값 유효성 검증)")
        @WithMockUser
        void findCursor_fail_validation(String limit, String sortDirection, String sortBy,
                        String errorField) throws Exception {
                var requestBuilder = get("/api/playlists");

                if (limit != null) {
                        requestBuilder.param("limit", limit);
                }
                if (sortDirection != null) {
                        requestBuilder.param("sortDirection", sortDirection);
                }
                if (sortBy != null) {
                        requestBuilder.param("sortBy", sortBy);
                }

                mockMvc.perform(requestBuilder).andDo(print()).andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                                .andExpect(jsonPath("$.details." + errorField).exists());
        }

        @Test
        @DisplayName("커서 기반 조회 - 실패 (선택 파라미터 UUID 포맷 오류)")
        @WithMockUser
        void findCursor_fail_invalid_uuid_format() throws Exception {
                mockMvc.perform(get("/api/playlists").param("limit", "10")
                                .param("sortBy", "UPDATED_AT").param("sortDirection", "DESC")
                                .param("ownerIdEqual", "not_a_uuid")).andDo(print())
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                                .andExpect(jsonPath("$.details.ownerIdEqual").exists());
        }

        @Test
        @DisplayName("커서 기반 조회 - 실패 (유효하지 않은 정렬 기준)")
        @WithMockUser
        void findCursor_fail_PlaylistIncorrectSortByException() throws Exception {
                // given
                willThrow(new PlaylistIncorrectSortByException("INVALID_SORT"))
                                .given(playlistMapper).toCommand(any());

                // when & then
                mockMvc.perform(get("/api/playlists").param("limit", "10")
                                .param("sortBy", "INVALID_SORT").param("sortDirection", "DESC"))
                                .andDo(print()).andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.exceptionType")
                                                .value("PlaylistIncorrectSortByException"))
                                .andExpect(jsonPath("$.message").value("SortBy 입력값이 올바르지 않습니다."));
        }

        @ParameterizedTest
        @CsvSource({"UPDATED_AT, not_a_date", "SUBSCRIBE_COUNT, not_a_number"})
        @DisplayName("커서 기반 조회 - 실패 (정렬 기준과 커서 타입 불일치)")
        @WithMockUser
        void findCursor_fail_PlaylistSortByMismatchException(String sortBy, String cursor)
                        throws Exception {
                // given
                willThrow(new PlaylistSortByMismatchException(sortBy, cursor)).given(playlistMapper)
                                .toCommand(any());

                // when & then
                mockMvc.perform(get("/api/playlists").param("limit", "10").param("sortBy", sortBy)
                                .param("sortDirection", "DESC").param("cursor", cursor))
                                .andDo(print()).andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.exceptionType")
                                                .value("PlaylistSortByMismatchException"))
                                .andExpect(jsonPath("$.message").value(
                                                "정렬 기준과 커서 타입이 일치하지 않습니다. (허용 포맷: updatedAt -> 예: 2026-07-01T10:00:00Z, subscribeCount -> 숫자)"));
        }

        @Test
        @DisplayName("플레이리스트 생성")
        @WithMockUser(username = "user@test.com")
        void create() throws Exception {
                // given
                PlaylistCreateRequest request = new PlaylistCreateRequest("New Playlist", "Desc");
                given(playlistService.create(eq("user@test.com"), any(PlaylistCreateRequest.class)))
                                .willReturn(playlistResponse);

                // when & then
                mockMvc.perform(post("/api/playlists").with(csrf()).principal(principal)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))).andDo(print())
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(playlistId.toString()));

                verify(playlistService).create(eq("user@test.com"), createRequestCaptor.capture());
                PlaylistCreateRequest captured = createRequestCaptor.getValue();
                assertThat(captured.title()).isEqualTo("New Playlist");
                assertThat(captured.description()).isEqualTo("Desc");
        }

        @Test
        @DisplayName("플레이리스트 수정")
        @WithMockUser(username = "user@test.com")
        void update() throws Exception {
                // given
                PlaylistUpdateRequest request =
                                new PlaylistUpdateRequest("Updated Playlist", "Updated Desc");
                given(playlistService.update(eq(playlistId), eq("user@test.com"),
                                any(PlaylistUpdateRequest.class))).willReturn(playlistResponse);

                // when & then
                mockMvc.perform(patch("/api/playlists/{id}", playlistId).with(csrf())
                                .principal(principal).contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))).andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(playlistId.toString()));

                verify(playlistService).update(eq(playlistId), eq("user@test.com"),
                                updateRequestCaptor.capture());
                PlaylistUpdateRequest captured = updateRequestCaptor.getValue();
                assertThat(captured.title()).isEqualTo("Updated Playlist");
                assertThat(captured.description()).isEqualTo("Updated Desc");
        }

        @Test
        @DisplayName("플레이리스트 삭제")
        @WithMockUser(username = "user@test.com")
        void deletePlaylist() throws Exception {
                // given
                willDoNothing().given(playlistService).delete(playlistId, "user@test.com");

                // when & then
                mockMvc.perform(delete("/api/playlists/{id}", playlistId).with(csrf())
                                .principal(principal)).andDo(print())
                                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("플레이리스트 삭제 - 실패 (권한 없음)")
        @WithMockUser(username = "user@test.com")
        void deletePlaylist_fail_PlaylistAccessDeniedException() throws Exception {
                // given
                willThrow(new PlaylistAccessDeniedException(playlistId, "user@test.com"))
                                .given(playlistService).delete(playlistId, "user@test.com");

                // when & then
                mockMvc.perform(delete("/api/playlists/{id}", playlistId).with(csrf())
                                .principal(principal)).andDo(print())
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.exceptionType")
                                                .value("PlaylistAccessDeniedException"))
                                .andExpect(jsonPath("$.message").value("플레이리스트 접근 권한이 없습니다."));
        }

        @Test
        @DisplayName("플레이리스트 아이템 추가")
        @WithMockUser(username = "user@test.com")
        void addContent() throws Exception {
                // given
                willDoNothing().given(playlistService).addContent("user@test.com", playlistId,
                                contentId);

                // when & then
                mockMvc.perform(post("/api/playlists/{playlistId}/contents/{contentId}", playlistId,
                                contentId).with(csrf()).principal(principal)).andDo(print())
                                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("플레이리스트 아이템 추가 - 실패 (컨텐츠 없음)")
        @WithMockUser(username = "user@test.com")
        void addContent_fail_PlaylistContentNotFoundException() throws Exception {
                // given
                willThrow(new PlaylistContentNotFoundException(contentId)).given(playlistService)
                                .addContent("user@test.com", playlistId, contentId);

                // when & then
                mockMvc.perform(post("/api/playlists/{playlistId}/contents/{contentId}", playlistId,
                                contentId).with(csrf()).principal(principal)).andDo(print())
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.exceptionType")
                                                .value("PlaylistContentNotFoundException"))
                                .andExpect(jsonPath("$.message").value("컨텐츠를 찾을 수 없습니다."));
        }

        @Test
        @DisplayName("플레이리스트 아이템 삭제")
        @WithMockUser(username = "user@test.com")
        void removeContent() throws Exception {
                // given
                willDoNothing().given(playlistService).removeContent("user@test.com", playlistId,
                                contentId);

                // when & then
                mockMvc.perform(delete("/api/playlists/{playlistId}/contents/{contentId}",
                                playlistId, contentId).with(csrf()).principal(principal))
                                .andDo(print()).andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("플레이리스트 아이템 삭제 - 실패 (아이템 없음)")
        @WithMockUser(username = "user@test.com")
        void removeContent_fail_PlaylistItemNotFoundException() throws Exception {
                // given
                willThrow(new PlaylistItemNotFoundException(playlistId, contentId))
                                .given(playlistService)
                                .removeContent("user@test.com", playlistId, contentId);

                // when & then
                mockMvc.perform(delete("/api/playlists/{playlistId}/contents/{contentId}",
                                playlistId, contentId).with(csrf()).principal(principal))
                                .andDo(print()).andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.exceptionType")
                                                .value("PlaylistItemNotFoundException"))
                                .andExpect(jsonPath("$.message")
                                                .value("플레이리스트에 추가된 컨텐츠를 찾을 수 없습니다."));
        }
}
