package com.codeit.team5.mopl.content.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.team5.mopl.TestGlobalExceptionHandlerConfig;
import com.codeit.team5.mopl.auth.filter.JwtAuthenticationFilter;
import com.codeit.team5.mopl.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
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

    @MockitoBean(name = "asyncJobLauncher")
    private JobLauncher asyncJobLauncher;

    @MockitoBean(name = "tmdbMovieJob")
    private Job tmdbMovieJob;

    @MockitoBean(name = "tmdbTvSeriesJob")
    private Job tmdbTvSeriesJob;

    @MockitoBean(name = "sportsDbEventJob")
    private Job sportsDbEventJob;

    @MockitoBean(name = "sportsDbDayJob")
    private Job sportsDbDayJob;

    // --- TMDB 영화 수집 ---

    @Test
    @DisplayName("TMDB 영화 수집 요청이면 202 응답을 반환한다")
    void collectTmdbMovies_success() throws Exception {
        mockMvc.perform(post("/api/admin/contents/collect/tmdb/movies")
                        .param("startPage", "1")
                        .param("endPage", "5"))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("TMDB 영화 수집 파라미터 생략 시 400 응답을 반환한다")
    void collectTmdbMovies_missingParams_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/admin/contents/collect/tmdb/movies"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TMDB 영화 수집 시 startPage가 0 이하면 400 응답을 반환한다")
    void collectTmdbMovies_invalidStartPage_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/admin/contents/collect/tmdb/movies")
                        .param("startPage", "0")
                        .param("endPage", "5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TMDB 영화 수집 시 endPage가 startPage보다 작으면 400 응답을 반환한다")
    void collectTmdbMovies_endPageLessThanStartPage_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/admin/contents/collect/tmdb/movies")
                        .param("startPage", "5")
                        .param("endPage", "3"))
                .andExpect(status().isBadRequest());
    }

    // --- TMDB TV 시리즈 수집 ---

    @Test
    @DisplayName("TMDB TV 시리즈 수집 요청이면 202 응답을 반환한다")
    void collectTmdbTvSeries_success() throws Exception {
        mockMvc.perform(post("/api/admin/contents/collect/tmdb/tv")
                        .param("startPage", "2")
                        .param("endPage", "10"))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("TMDB TV 수집 파라미터 생략 시 400 응답을 반환한다")
    void collectTmdbTvSeries_missingParams_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/admin/contents/collect/tmdb/tv"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TMDB TV 수집 시 endPage가 startPage보다 작으면 400 응답을 반환한다")
    void collectTmdbTvSeries_endPageLessThanStartPage_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/admin/contents/collect/tmdb/tv")
                        .param("startPage", "10")
                        .param("endPage", "5"))
                .andExpect(status().isBadRequest());
    }

    // --- SportsDB 경기 수집 ---

    @Test
    @DisplayName("SportsDB 경기 수집 요청이면 202 응답을 반환한다")
    void collectSportsEvents_success() throws Exception {
        mockMvc.perform(post("/api/admin/contents/collect/sports")
                        .param("league", "EPL")
                        .param("season", "2023-2024"))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("SportsDB league 파라미터가 누락되면 400 응답을 반환한다")
    void collectSportsEvents_missingLeague_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/admin/contents/collect/sports")
                        .param("season", "2023-2024"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("SportsDB season 파라미터가 누락되면 400 응답을 반환한다")
    void collectSportsEvents_missingSeason_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/admin/contents/collect/sports")
                        .param("league", "EPL"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("SportsDB season 형식이 YYYY-YYYY가 아니면 400 응답을 반환한다")
    void collectSportsEvents_invalidSeasonFormat_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/admin/contents/collect/sports")
                        .param("league", "EPL")
                        .param("season", "2023/2024"))
                .andExpect(status().isBadRequest());
    }

    // --- SportsDB 일별 경기 수집 ---

    @Test
    @DisplayName("SportsDB 일별 경기 수집 요청이면 202 응답을 반환한다")
    void collectSportsEventsByDay_success() throws Exception {
        mockMvc.perform(post("/api/admin/contents/collect/sports/day")
                        .param("date", "2024-12-26"))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("SportsDB 일별 경기 수집 시 date 파라미터가 누락되면 400 응답을 반환한다")
    void collectSportsEventsByDay_missingDate_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/admin/contents/collect/sports/day"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("SportsDB 일별 경기 수집 시 date 형식이 YYYY-MM-DD가 아니면 400 응답을 반환한다")
    void collectSportsEventsByDay_invalidDateFormat_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/admin/contents/collect/sports/day")
                        .param("date", "2024/12/26"))
                .andExpect(status().isBadRequest());
    }
}
