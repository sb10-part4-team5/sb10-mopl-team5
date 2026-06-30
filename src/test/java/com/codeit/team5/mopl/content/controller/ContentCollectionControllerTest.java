package com.codeit.team5.mopl.content.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.team5.mopl.TestGlobalExceptionHandlerConfig;
import com.codeit.team5.mopl.auth.filter.JwtAuthenticationFilter;
import com.codeit.team5.mopl.content.service.SportsDbContentService;
import com.codeit.team5.mopl.content.service.TmdbContentService;
import com.codeit.team5.mopl.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = ContentCollectionController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, TestGlobalExceptionHandlerConfig.class})
class ContentCollectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TmdbContentService tmdbContentService;

    @MockitoBean
    private SportsDbContentService sportsDbContentService;

    // --- TMDB 영화 수집 ---

    @Test
    @DisplayName("TMDB 영화 수집 요청이면 202 응답을 반환한다")
    void collectTmdbMovies_success() throws Exception {
        mockMvc.perform(post("/api/admin/contents/collect/tmdb/movies")
                        .param("startPage", "1")
                        .param("endPage", "5"))
                .andExpect(status().isAccepted());

        verify(tmdbContentService).collectMovies(1, 5);
    }

    @Test
    @DisplayName("TMDB 영화 수집 파라미터 생략 시 기본값(1, 1)으로 202 응답을 반환한다")
    void collectTmdbMovies_defaultParams_success() throws Exception {
        mockMvc.perform(post("/api/admin/contents/collect/tmdb/movies"))
                .andExpect(status().isAccepted());

        verify(tmdbContentService).collectMovies(1, 1);
    }

    @Test
    @DisplayName("TMDB 영화 수집 중 예상하지 못한 예외가 발생하면 500 응답을 반환한다")
    void collectTmdbMovies_unexpectedException_returnsInternalServerError() throws Exception {
        doThrow(new IllegalStateException("unexpected")).when(tmdbContentService).collectMovies(1, 1);

        mockMvc.perform(post("/api/admin/contents/collect/tmdb/movies"))
                .andExpect(status().isInternalServerError());
    }

    // --- TMDB TV 시리즈 수집 ---

    @Test
    @DisplayName("TMDB TV 시리즈 수집 요청이면 202 응답을 반환한다")
    void collectTmdbTvSeries_success() throws Exception {
        mockMvc.perform(post("/api/admin/contents/collect/tmdb/tv")
                        .param("startPage", "2")
                        .param("endPage", "10"))
                .andExpect(status().isAccepted());

        verify(tmdbContentService).collectTvSeries(2, 10);
    }

    @Test
    @DisplayName("TMDB TV 수집 파라미터 생략 시 기본값(1, 1)으로 202 응답을 반환한다")
    void collectTmdbTvSeries_defaultParams_success() throws Exception {
        mockMvc.perform(post("/api/admin/contents/collect/tmdb/tv"))
                .andExpect(status().isAccepted());

        verify(tmdbContentService).collectTvSeries(1, 1);
    }

    @Test
    @DisplayName("TMDB TV 수집 중 예상하지 못한 예외가 발생하면 500 응답을 반환한다")
    void collectTmdbTvSeries_unexpectedException_returnsInternalServerError() throws Exception {
        doThrow(new IllegalStateException("unexpected")).when(tmdbContentService).collectTvSeries(1, 1);

        mockMvc.perform(post("/api/admin/contents/collect/tmdb/tv"))
                .andExpect(status().isInternalServerError());
    }

    // --- SportsDB 경기 수집 ---

    @Test
    @DisplayName("SportsDB 경기 수집 요청이면 202 응답을 반환한다")
    void collectSportsEvents_success() throws Exception {
        mockMvc.perform(post("/api/admin/contents/collect/sports")
                        .param("league", "EPL")
                        .param("season", "2023-2024"))
                .andExpect(status().isAccepted());

        verify(sportsDbContentService).collectEvents("4328", "2023-2024");
    }

    @Test
    @DisplayName("SportsDB league 파라미터가 누락되면 500 응답을 반환한다")
    void collectSportsEvents_missingLeague_returnsError() throws Exception {
        mockMvc.perform(post("/api/admin/contents/collect/sports")
                        .param("season", "2023-2024"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("SportsDB season 파라미터가 누락되면 500 응답을 반환한다")
    void collectSportsEvents_missingSeason_returnsError() throws Exception {
        mockMvc.perform(post("/api/admin/contents/collect/sports")
                        .param("league", "EPL"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("SportsDB 경기 수집 중 예상하지 못한 예외가 발생하면 500 응답을 반환한다")
    void collectSportsEvents_unexpectedException_returnsInternalServerError() throws Exception {
        doThrow(new IllegalStateException("unexpected"))
                .when(sportsDbContentService).collectEvents("4328", "2023-2024");

        mockMvc.perform(post("/api/admin/contents/collect/sports")
                        .param("league", "EPL")
                        .param("season", "2023-2024"))
                .andExpect(status().isInternalServerError());
    }
}
