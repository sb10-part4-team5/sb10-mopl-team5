package com.codeit.team5.mopl.config;

import com.codeit.team5.mopl.binarycontent.repository.BinaryContentRepository;
import com.codeit.team5.mopl.content.batch.dto.ContentWithMetaData;
import com.codeit.team5.mopl.content.batch.processor.SportsDbEventItemProcessor;
import com.codeit.team5.mopl.content.batch.processor.TmdbMovieItemProcessor;
import com.codeit.team5.mopl.content.batch.processor.TmdbTvSeriesItemProcessor;
import com.codeit.team5.mopl.content.batch.reader.SportsDbDayItemReader;
import com.codeit.team5.mopl.content.batch.reader.SportsDbEventItemReader;
import com.codeit.team5.mopl.content.batch.reader.TmdbMovieItemReader;
import com.codeit.team5.mopl.content.batch.reader.TmdbTvSeriesItemReader;
import com.codeit.team5.mopl.content.batch.writer.ContentItemWriter;
import com.codeit.team5.mopl.content.client.sportsdb.SportsDbApiClient;
import com.codeit.team5.mopl.content.client.tmdb.TmdbApiClient;
import com.codeit.team5.mopl.content.dto.external.sportsdb.SportsDbEventDto;
import com.codeit.team5.mopl.content.dto.external.tmdb.TmdbMovieDto;
import com.codeit.team5.mopl.content.dto.external.tmdb.TmdbTvDto;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.content.repository.ContentStatsRepository;
import com.codeit.team5.mopl.tag.repository.TagRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.reactive.function.client.WebClientException;

@Configuration
@RequiredArgsConstructor
public class BatchConfig {

    private static final int CHUNK_SIZE = 100;
    private static final int RETRY_LIMIT = 3;
    private static final int TMDB_SKIP_LIMIT = 10;
    private static final int SPORTS_DB_SKIP_LIMIT = 3;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final TmdbApiClient tmdbApiClient;
    private final SportsDbApiClient sportsDbApiClient;
    private final ContentRepository contentRepository;
    private final ContentStatsRepository contentStatsRepository;
    private final BinaryContentRepository binaryContentRepository;
    private final TagRepository tagRepository;
    private final ObjectMapper objectMapper;

    // ── TMDB 영화 Job ──────────────────────────────────────────────

    @Bean
    public Job tmdbMovieJob() {
        return new JobBuilder("tmdbMovieJob", jobRepository)
                .start(tmdbMovieStep())
                .build();
    }

    @Bean
    public Step tmdbMovieStep() {
        return new StepBuilder("tmdbMovieStep", jobRepository)
                .<TmdbMovieDto, ContentWithMetaData>chunk(CHUNK_SIZE, transactionManager)
                .reader(tmdbMovieItemReader())
                .processor(tmdbMovieItemProcessor())
                .writer(contentItemWriter())
                .faultTolerant()
                .retryLimit(RETRY_LIMIT)
                .retry(WebClientException.class)
                .retry(TransientDataAccessException.class)
                .skipLimit(TMDB_SKIP_LIMIT)
                .skip(Exception.class)
                .noSkip(WebClientException.class)
                .noSkip(TransientDataAccessException.class)
                .build();
    }

    @Bean
    public TmdbMovieItemReader tmdbMovieItemReader() {
        return new TmdbMovieItemReader(tmdbApiClient);
    }

    @Bean
    public TmdbMovieItemProcessor tmdbMovieItemProcessor() {
        return new TmdbMovieItemProcessor(contentRepository, objectMapper);
    }

    // ── TMDB TV 시리즈 Job ─────────────────────────────────────────

    @Bean
    public Job tmdbTvSeriesJob() {
        return new JobBuilder("tmdbTvSeriesJob", jobRepository)
                .start(tmdbTvSeriesStep())
                .build();
    }

    @Bean
    public Step tmdbTvSeriesStep() {
        return new StepBuilder("tmdbTvSeriesStep", jobRepository)
                .<TmdbTvDto, ContentWithMetaData>chunk(CHUNK_SIZE, transactionManager)
                .reader(tmdbTvSeriesItemReader())
                .processor(tmdbTvSeriesItemProcessor())
                .writer(contentItemWriter())
                .faultTolerant()
                .retryLimit(RETRY_LIMIT)
                .retry(WebClientException.class)
                .retry(TransientDataAccessException.class)
                .skipLimit(TMDB_SKIP_LIMIT)
                .skip(Exception.class)
                .noSkip(WebClientException.class)
                .noSkip(TransientDataAccessException.class)
                .build();
    }

    @Bean
    public TmdbTvSeriesItemReader tmdbTvSeriesItemReader() {
        return new TmdbTvSeriesItemReader(tmdbApiClient);
    }

    @Bean
    public TmdbTvSeriesItemProcessor tmdbTvSeriesItemProcessor() {
        return new TmdbTvSeriesItemProcessor(contentRepository, objectMapper);
    }

    // ── SportsDB 리그-시즌 Job ───────────────────────────────────────────────

    @Bean
    public Job sportsDbEventJob() {
        return new JobBuilder("sportsDbEventJob", jobRepository)
                .start(sportsDbEventStep())
                .build();
    }

    @Bean
    public Step sportsDbEventStep() {
        return new StepBuilder("sportsDbEventStep", jobRepository)
                .<SportsDbEventDto, ContentWithMetaData>chunk(CHUNK_SIZE, transactionManager)
                .reader(sportsDbEventItemReader())
                .processor(sportsDbEventItemProcessor())
                .writer(contentItemWriter())
                .faultTolerant()
                .retryLimit(RETRY_LIMIT)
                .retry(WebClientException.class)
                .retry(TransientDataAccessException.class)
                .skipLimit(SPORTS_DB_SKIP_LIMIT)
                .skip(Exception.class)
                .noSkip(WebClientException.class)
                .noSkip(TransientDataAccessException.class)
                .build();
    }

    @Bean
    public SportsDbEventItemReader sportsDbEventItemReader() {
        return new SportsDbEventItemReader(sportsDbApiClient);
    }

    @Bean
    public SportsDbEventItemProcessor sportsDbEventItemProcessor() {
        return new SportsDbEventItemProcessor(contentRepository, objectMapper);
    }

    // ── SportsDB 일별 Job ──────────────────────────────────────────

    @Bean
    public Job sportsDbDayJob() {
        return new JobBuilder("sportsDbDayJob", jobRepository)
                .start(sportsDbDayStep())
                .build();
    }

    @Bean
    public Step sportsDbDayStep() {
        return new StepBuilder("sportsDbDayStep", jobRepository)
                .<SportsDbEventDto, ContentWithMetaData>chunk(CHUNK_SIZE, transactionManager)
                .reader(sportsDbDayItemReader())
                .processor(sportsDbEventItemProcessor())
                .writer(contentItemWriter())
                .faultTolerant()
                .retryLimit(RETRY_LIMIT)
                .retry(WebClientException.class)
                .retry(TransientDataAccessException.class)
                .skipLimit(SPORTS_DB_SKIP_LIMIT)
                .skip(Exception.class)
                .noSkip(WebClientException.class)
                .noSkip(TransientDataAccessException.class)
                .build();
    }

    @Bean
    public SportsDbDayItemReader sportsDbDayItemReader() {
        return new SportsDbDayItemReader(sportsDbApiClient);
    }

    // ── 공통 Writer ────────────────────────────────────────────────

    @Bean
    public ContentItemWriter contentItemWriter() {
        return new ContentItemWriter(contentRepository, contentStatsRepository,
                binaryContentRepository, tagRepository);
    }
}
