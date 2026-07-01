package com.codeit.team5.mopl.content.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.team5.mopl.TestGlobalExceptionHandlerConfig;
import com.codeit.team5.mopl.auth.filter.JwtAuthenticationFilter;
import com.codeit.team5.mopl.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
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
    @DisplayName("TMDB 영화 수집 파라미터 생략 시 기본값으로 202 응답을 반환한다")
    void collectTmdbMovies_defaultParams_success() throws Exception {
        mockMvc.perform(post("/api/admin/contents/collect/tmdb/movies"))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("TMDB 영화 수집 중 Job 실행 예외가 발생해도 202 응답을 반환한다")
    void collectTmdbMovies_jobException_returnsAccepted() throws Exception {
        asyncJobLauncher.run(tmdbMovieJob, new JobParameters());

        mockMvc.perform(post("/api/admin/contents/collect/tmdb/movies"))
                .andExpect(status().isAccepted());
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
    @DisplayName("TMDB TV 수집 파라미터 생략 시 기본값으로 202 응답을 반환한다")
    void collectTmdbTvSeries_defaultParams_success() throws Exception {
        mockMvc.perform(post("/api/admin/contents/collect/tmdb/tv"))
                .andExpect(status().isAccepted());
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
}
