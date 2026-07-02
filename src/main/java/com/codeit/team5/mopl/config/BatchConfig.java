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
import com.codeit.team5.mopl.content.batch.retry.LoggingSkipListener;
import com.codeit.team5.mopl.content.batch.retry.SelectiveRetryPolicy;
import com.codeit.team5.mopl.content.batch.retry.SelectiveSkipPolicy;
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
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class BatchConfig {

    private static final int CHUNK_SIZE = 100;
    private static final int RETRY_LIMIT = 3;
    private static final int TMDB_SKIP_LIMIT = 10;
    private static final int SPORTS_DB_SKIP_LIMIT = 3;
    private static final int EXTERNAL_FETCH_MAX_ATTEMPTS = 2;
    private static final long EXTERNAL_FETCH_BACKOFF_MS = 200;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final TmdbApiClient tmdbApiClient;
    private final SportsDbApiClient sportsDbApiClient;
    private final ContentRepository contentRepository;
    private final ContentStatsRepository contentStatsRepository;
    private final BinaryContentRepository binaryContentRepository;
    private final TagRepository tagRepository;
    private final ObjectMapper objectMapper;

    // ── 비동기 JobLauncher ───────────────────────────────

    @Bean
    public JobLauncher asyncJobLauncher(JobRepository jobRepository,
            ThreadPoolTaskExecutor batchJobTaskExecutor) throws Exception {
        TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(jobRepository);
        launcher.setTaskExecutor(batchJobTaskExecutor);
        launcher.afterPropertiesSet();
        return launcher;
    }

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
                .retryPolicy(new SelectiveRetryPolicy(RETRY_LIMIT))
                .skipPolicy(new SelectiveSkipPolicy(TMDB_SKIP_LIMIT))
                .listener(new LoggingSkipListener())
                .build();
    }

    @Bean
    @StepScope
    public TmdbMovieItemReader tmdbMovieItemReader() {
        return new TmdbMovieItemReader(tmdbApiClient, externalFetchRetryTemplate());
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
                .retryPolicy(new SelectiveRetryPolicy(RETRY_LIMIT))
                .skipPolicy(new SelectiveSkipPolicy(TMDB_SKIP_LIMIT))
                .listener(new LoggingSkipListener())
                .build();
    }

    @Bean
    @StepScope
    public TmdbTvSeriesItemReader tmdbTvSeriesItemReader() {
        return new TmdbTvSeriesItemReader(tmdbApiClient, externalFetchRetryTemplate());
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
                .retryPolicy(new SelectiveRetryPolicy(RETRY_LIMIT))
                .skipPolicy(new SelectiveSkipPolicy(SPORTS_DB_SKIP_LIMIT))
                .listener(new LoggingSkipListener())
                .build();
    }

    @Bean
    @StepScope
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
                .retryPolicy(new SelectiveRetryPolicy(RETRY_LIMIT))
                .skipPolicy(new SelectiveSkipPolicy(SPORTS_DB_SKIP_LIMIT))
                .listener(new LoggingSkipListener())
                .build();
    }

    @Bean
    @StepScope
    public SportsDbDayItemReader sportsDbDayItemReader() {
        return new SportsDbDayItemReader(sportsDbApiClient, externalFetchRetryTemplate());
    }

    // @BeforeStep에서 여러 건을 순회 조회하는 Reader(TMDB 영화/TV, SportsDB 일별)가 공용으로 쓰는
    // 재시도 정책 (최대 2회 시도, 200ms 고정 백오프). @BeforeStep은 청크 단위 faultTolerant 재시도의
    // 보호 범위 밖이라, 한 건의 일시적 오류로 전체 수집이 실패해 이미 모은 데이터까지 유실되는 것을
    // 막기 위해 재시도해도 성공 가능성이 있는 오류만 재시도한다.
    @Bean
    public RetryTemplate externalFetchRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new SelectiveRetryPolicy(EXTERNAL_FETCH_MAX_ATTEMPTS));

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(EXTERNAL_FETCH_BACKOFF_MS);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }

    // ── 공통 Writer ────────────────────────────────────────────────

    @Bean
    public ContentItemWriter contentItemWriter() {
        return new ContentItemWriter(contentRepository, contentStatsRepository,
                binaryContentRepository, tagRepository);
    }
}
