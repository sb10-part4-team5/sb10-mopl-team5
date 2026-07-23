package com.codeit.team5.mopl.content.batch.scheduler;

import java.time.LocalDate;
import java.time.ZoneId;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ContentCollectionScheduler {

    private final JobLauncher asyncJobLauncher;
    private final Job tmdbMovieJob;
    private final Job tmdbTvSeriesJob;
    private final Job sportsDbDayJob;
    private final int dailyEndPage;

    public ContentCollectionScheduler(
            @Qualifier("asyncJobLauncher") JobLauncher asyncJobLauncher,
            @Qualifier("tmdbMovieJob") Job tmdbMovieJob,
            @Qualifier("tmdbTvSeriesJob") Job tmdbTvSeriesJob,
            @Qualifier("sportsDbDayJob") Job sportsDbDayJob,
            @Value("${tmdb.batch.daily-end-page}") int dailyEndPage
    ) {
        this.asyncJobLauncher = asyncJobLauncher;
        this.tmdbMovieJob = tmdbMovieJob;
        this.tmdbTvSeriesJob = tmdbTvSeriesJob;
        this.sportsDbDayJob = sportsDbDayJob;
        this.dailyEndPage = dailyEndPage;
    }

    // asyncJobLauncher는 잡을 던지자마자 반환돼 락 보유 시간이 실제 수집 시간을 반영하지 못한다.
    // lockAtLeastFor로 다른 인스턴스의 같은 시각 cron 트리거를 강제로 막는다.
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "tmdbMovieCollection", lockAtMostFor = "15m", lockAtLeastFor = "10m")
    public void runTmdbMovieJob() {
        log.info("[Scheduler] TMDB 영화 수집 시작");
        run(tmdbMovieJob, new JobParametersBuilder()
                .addString("startPage", "1")
                .addString("endPage", String.valueOf(dailyEndPage))
                .addString("run.id", String.valueOf(System.currentTimeMillis()))
                .toJobParameters());
    }

    @Scheduled(cron = "0 3 3 * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "tmdbTvSeriesCollection", lockAtMostFor = "15m", lockAtLeastFor = "10m")
    public void runTmdbTvSeriesJob() {
        log.info("[Scheduler] TMDB TV 시리즈 수집 시작");
        run(tmdbTvSeriesJob, new JobParametersBuilder()
                .addString("startPage", "1")
                .addString("endPage", String.valueOf(dailyEndPage))
                .addString("run.id", String.valueOf(System.currentTimeMillis()))
                .toJobParameters());
    }

    @Scheduled(cron = "0 6 3 * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "sportsDbDayCollection", lockAtMostFor = "15m", lockAtLeastFor = "10m")
    public void runSportsDbDayJob() {
        log.info("[Scheduler] SportsDB 일별 경기 수집 시작");
        run(sportsDbDayJob, new JobParametersBuilder()
                .addString("date", LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1).toString())
                .addString("run.id", String.valueOf(System.currentTimeMillis()))
                .toJobParameters());
    }

private void run(Job job, JobParameters params) {
        try {
            asyncJobLauncher.run(job, params);
        } catch (Exception e) {
            log.error("[Scheduler] Job 실행 실패 - job={}, error={}", job.getName(), e.getMessage(), e);
        }
    }
}
