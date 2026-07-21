package com.codeit.team5.mopl.content.batch.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;

@ExtendWith(MockitoExtension.class)
class ContentCollectionSchedulerTest {

    private static final int DAILY_END_PAGE = 5;

    @Mock
    private JobLauncher asyncJobLauncher;

    @Mock
    private Job tmdbMovieJob;

    @Mock
    private Job tmdbTvSeriesJob;

    @Mock
    private Job sportsDbDayJob;

    private ContentCollectionScheduler scheduler;

    @Captor
    private ArgumentCaptor<JobParameters> paramsCaptor;

    @BeforeEach
    void setUp() {
        scheduler = new ContentCollectionScheduler(
                asyncJobLauncher, tmdbMovieJob, tmdbTvSeriesJob, sportsDbDayJob, DAILY_END_PAGE);
    }

    @Test
    @DisplayName("TMDB 영화 수집은 1페이지부터 설정된 종료 페이지까지 파라미터로 잡을 실행한다")
    void runTmdbMovieJob_launchesWithConfiguredPageRange() throws Exception {
        // when
        scheduler.runTmdbMovieJob();

        // then
        verify(asyncJobLauncher).run(eq(tmdbMovieJob), paramsCaptor.capture());
        JobParameters params = paramsCaptor.getValue();
        assertThat(params.getString("startPage")).isEqualTo("1");
        assertThat(params.getString("endPage")).isEqualTo(String.valueOf(DAILY_END_PAGE));
    }

    @Test
    @DisplayName("TMDB TV 시리즈 수집은 1페이지부터 설정된 종료 페이지까지 파라미터로 잡을 실행한다")
    void runTmdbTvSeriesJob_launchesWithConfiguredPageRange() throws Exception {
        // when
        scheduler.runTmdbTvSeriesJob();

        // then
        verify(asyncJobLauncher).run(eq(tmdbTvSeriesJob), paramsCaptor.capture());
        JobParameters params = paramsCaptor.getValue();
        assertThat(params.getString("startPage")).isEqualTo("1");
        assertThat(params.getString("endPage")).isEqualTo(String.valueOf(DAILY_END_PAGE));
    }

    @Test
    @DisplayName("SportsDB 일별 수집은 어제 날짜를 파라미터로 잡을 실행한다")
    void runSportsDbDayJob_launchesWithYesterdayDate() throws Exception {
        // given
        String expectedDate = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1).toString();

        // when
        scheduler.runSportsDbDayJob();

        // then
        verify(asyncJobLauncher).run(eq(sportsDbDayJob), paramsCaptor.capture());
        assertThat(paramsCaptor.getValue().getString("date")).isEqualTo(expectedDate);
    }

    @Test
    @DisplayName("잡 실행 중 예외가 발생해도 스케줄러 메서드 밖으로 전파되지 않는다")
    void run_swallowsJobLauncherException() throws Exception {
        // given
        given(asyncJobLauncher.run(any(), any()))
                .willThrow(new JobParametersInvalidException("잘못된 Job 파라미터"));

        // when & then
        assertThatCode(() -> scheduler.runTmdbMovieJob()).doesNotThrowAnyException();
    }
}
