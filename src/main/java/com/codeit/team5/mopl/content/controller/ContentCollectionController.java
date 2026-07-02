package com.codeit.team5.mopl.content.controller;

import com.codeit.team5.mopl.content.batch.exception.BatchJobLaunchException;
import com.codeit.team5.mopl.content.controller.api.ContentCollectionApi;
import com.codeit.team5.mopl.content.dto.external.sportsdb.SportsDbLeague;
import com.codeit.team5.mopl.content.dto.request.PageRangeRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/admin/contents/collect")
public class ContentCollectionController implements ContentCollectionApi {

    private final JobLauncher asyncJobLauncher;
    private final Job tmdbMovieJob;
    private final Job tmdbTvSeriesJob;
    private final Job sportsDbEventJob;
    private final Job sportsDbDayJob;

    public ContentCollectionController(
            @Qualifier("asyncJobLauncher") JobLauncher asyncJobLauncher,
            @Qualifier("tmdbMovieJob") Job tmdbMovieJob,
            @Qualifier("tmdbTvSeriesJob") Job tmdbTvSeriesJob,
            @Qualifier("sportsDbEventJob") Job sportsDbEventJob,
            @Qualifier("sportsDbDayJob") Job sportsDbDayJob
    ) {
        this.asyncJobLauncher = asyncJobLauncher;
        this.tmdbMovieJob = tmdbMovieJob;
        this.tmdbTvSeriesJob = tmdbTvSeriesJob;
        this.sportsDbEventJob = sportsDbEventJob;
        this.sportsDbDayJob = sportsDbDayJob;
    }

    @PostMapping("/tmdb/movies")
    public ResponseEntity<Void> collectTmdbMovies(@Valid @ModelAttribute PageRangeRequest request) {
        log.info("TMDB 영화 수집 요청: {}~{}페이지", request.startPage(), request.endPage());
        run(tmdbMovieJob, new JobParametersBuilder()
                .addString("startPage", String.valueOf(request.startPage()))
                .addString("endPage", String.valueOf(request.endPage()))
                .addString("run.id", String.valueOf(System.currentTimeMillis()))
                .toJobParameters());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/tmdb/tv")
    public ResponseEntity<Void> collectTmdbTvSeries(@Valid @ModelAttribute PageRangeRequest request) {
        log.info("TMDB TV 시리즈 수집 요청: {}~{}페이지", request.startPage(), request.endPage());
        run(tmdbTvSeriesJob, new JobParametersBuilder()
                .addString("startPage", String.valueOf(request.startPage()))
                .addString("endPage", String.valueOf(request.endPage()))
                .addString("run.id", String.valueOf(System.currentTimeMillis()))
                .toJobParameters());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/sports")
    public ResponseEntity<Void> collectSportsEvents(
            @RequestParam SportsDbLeague league,
            @Pattern(regexp = "^\\d{4}-\\d{4}$", message = "시즌 형식은 YYYY-YYYY이어야 합니다. (예: 2023-2024)")
            @RequestParam String season
    ) {
        log.info("SportsDB 경기 수집 요청: league={}, season={}", league.getName(), season);
        run(sportsDbEventJob, new JobParametersBuilder()
                .addString("leagueId", league.getLeagueId())
                .addString("season", season)
                .addString("run.id", String.valueOf(System.currentTimeMillis()))
                .toJobParameters());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/sports/day")
    public ResponseEntity<Void> collectSportsEventsByDay(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam LocalDate date
    ) {
        log.info("SportsDB 일별 경기 수집 요청: date={}", date);
        run(sportsDbDayJob, new JobParametersBuilder()
                .addString("date", date.toString())
                .addString("run.id", String.valueOf(System.currentTimeMillis()))
                .toJobParameters());
        return ResponseEntity.accepted().build();
    }

    private void run(Job job, JobParameters params) {
        try {
            asyncJobLauncher.run(job, params);
        } catch (JobExecutionAlreadyRunningException | JobRestartException |
                 JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {
            log.error("[Controller] Job 실행 실패 - job={}, error={}", job.getName(), e.getMessage(), e);
            throw new BatchJobLaunchException(job.getName());
        }
    }
}
