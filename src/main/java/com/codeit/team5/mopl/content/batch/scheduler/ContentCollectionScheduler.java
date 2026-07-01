package com.codeit.team5.mopl.content.batch.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ContentCollectionScheduler {

    private final JobLauncher jobLauncher;
    private final Job tmdbMovieJob;
    private final Job tmdbTvSeriesJob;
    private final Job sportsDbDayJob;

    @Value("${tmdb.batch.daily-end-page}")
    private int dailyEndPage;

    public ContentCollectionScheduler(
            JobLauncher jobLauncher,
            @Qualifier("tmdbMovieJob") Job tmdbMovieJob,
            @Qualifier("tmdbTvSeriesJob") Job tmdbTvSeriesJob,
            @Qualifier("sportsDbDayJob") Job sportsDbDayJob
    ) {
        this.jobLauncher = jobLauncher;
        this.tmdbMovieJob = tmdbMovieJob;
        this.tmdbTvSeriesJob = tmdbTvSeriesJob;
        this.sportsDbDayJob = sportsDbDayJob;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void runTmdbMovieJob() {
        log.info("[Scheduler] TMDB 영화 수집 시작");
        run(tmdbMovieJob, new JobParametersBuilder()
                .addString("startPage", "1")
                .addString("endPage", String.valueOf(dailyEndPage))
                .addString("run.id", String.valueOf(System.currentTimeMillis()))
                .toJobParameters());
    }

    @Scheduled(cron = "0 5 3 * * *")
    public void runTmdbTvSeriesJob() {
        log.info("[Scheduler] TMDB TV 시리즈 수집 시작");
        run(tmdbTvSeriesJob, new JobParametersBuilder()
                .addString("startPage", "1")
                .addString("endPage", String.valueOf(dailyEndPage))
                .addString("run.id", String.valueOf(System.currentTimeMillis()))
                .toJobParameters());
    }

    @Scheduled(cron = "0 10 3 * * *")
    public void runSportsDbDayJob() {
        log.info("[Scheduler] SportsDB 일별 경기 수집 시작");
        run(sportsDbDayJob, new JobParametersBuilder()
                .addString("run.id", String.valueOf(System.currentTimeMillis()))
                .toJobParameters());
    }

private void run(Job job, JobParameters params) {
        try {
            jobLauncher.run(job, params);
        } catch (JobExecutionAlreadyRunningException | JobRestartException |
                 JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {
            log.error("[Scheduler] Job 실행 실패 - job={}, error={}", job.getName(), e.getMessage());
        }
    }
}
