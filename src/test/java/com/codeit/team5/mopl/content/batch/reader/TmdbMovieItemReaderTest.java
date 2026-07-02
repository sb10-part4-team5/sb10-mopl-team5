package com.codeit.team5.mopl.content.batch.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.content.client.tmdb.TmdbApiClient;
import com.codeit.team5.mopl.content.dto.external.tmdb.TmdbMovieDto;
import com.codeit.team5.mopl.content.dto.external.tmdb.TmdbMovieListResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.retry.support.RetryTemplate;

@ExtendWith(MockitoExtension.class)
class TmdbMovieItemReaderTest {

    @Mock
    private TmdbApiClient tmdbApiClient;

    private TmdbMovieItemReader reader;

    @BeforeEach
    void setUp() {
        // 운영 설정(BatchConfig)과 동일하게 최대 2회 시도하되, 테스트 속도를 위해
        // 백오프(운영은 200ms)는 두지 않는다.
        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(2)
                .noBackoff()
                .retryOn(Exception.class)
                .build();
        reader = new TmdbMovieItemReader(tmdbApiClient, retryTemplate);
    }

    private StepExecution createStepExecution(String startPage, String endPage) {
        return MetaDataInstanceFactory.createStepExecution(
                new JobParametersBuilder()
                        .addString("startPage", startPage)
                        .addString("endPage", endPage)
                        .toJobParameters()
        );
    }

    @Test
    @DisplayName("startPage, endPage 파라미터가 없으면 예외가 발생한다")
    void beforeStep_missingParams_throwsException() {
        // given
        StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution();

        // when, then
        assertThatThrownBy(() -> reader.beforeStep(stepExecution))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("1페이지 데이터를 순서대로 읽는다")
    void read_returnsItemsInOrder() throws Exception {
        // given
        TmdbMovieDto movie1 = new TmdbMovieDto(1L, "영화1", "Movie1", "desc", null, List.of(), "2024-01-01", 7.0, "ko");
        TmdbMovieDto movie2 = new TmdbMovieDto(2L, "영화2", "Movie2", "desc", null, List.of(), "2024-01-01", 7.0, "ko");
        given(tmdbApiClient.fetchMovies(1)).willReturn(new TmdbMovieListResponse(1, List.of(movie1, movie2), 1, 2));

        // when
        reader.beforeStep(createStepExecution("1", "1"));

        // then
        assertThat(reader.read()).isEqualTo(movie1);
        assertThat(reader.read()).isEqualTo(movie2);
        assertThat(reader.read()).isNull();
    }

    @Test
    @DisplayName("title이 빈 영화는 필터링된다")
    void read_emptyTitle_filtered() throws Exception {
        // given
        TmdbMovieDto validMovie = new TmdbMovieDto(1L, "영화1", "Movie1", "desc", null, List.of(), "2024-01-01", 7.0, "ko");
        TmdbMovieDto emptyTitle = new TmdbMovieDto(2L, "", "NoTitle", "desc", null, List.of(), "2024-01-01", 7.0, "ko");
        given(tmdbApiClient.fetchMovies(1)).willReturn(new TmdbMovieListResponse(1, List.of(validMovie, emptyTitle), 1, 2));

        // when
        reader.beforeStep(createStepExecution("1", "1"));

        // then
        assertThat(reader.read()).isEqualTo(validMovie);
        assertThat(reader.read()).isNull();
    }

    @Test
    @DisplayName("endPage가 MAX_PAGE(500)를 초과하면 500으로 클램핑되어 501페이지부터는 API를 호출하지 않는다")
    void beforeStep_endPageExceedsMax_clampsTo500() throws Exception {
        // given
        // startPage를 501로 시작시켜, 클램핑된 endPage(500)를 즉시 넘어서는지로 검증한다.

        // when
        reader.beforeStep(createStepExecution("501", "9999"));

        // then
        assertThat(reader.read()).isNull();
        verify(tmdbApiClient, never()).fetchMovies(anyInt());
    }

    @Test
    @DisplayName("응답 results가 비어있으면 totalPages와 무관하게 즉시 종료하고 이후 페이지는 호출하지 않는다")
    void read_emptyResults_stopsImmediatelyWithoutCallingLaterPages() throws Exception {
        // given
        // totalPages를 크게 줘서, endPage 도달이 아니라 빈 results 자체 때문에 멈추는지 검증한다.
        given(tmdbApiClient.fetchMovies(1)).willReturn(new TmdbMovieListResponse(1, List.of(), 500, 0));

        // when
        reader.beforeStep(createStepExecution("1", "500"));

        // then
        assertThat(reader.read()).isNull();
        verify(tmdbApiClient, never()).fetchMovies(2);
    }

    @Test
    @DisplayName("페이지 조회가 첫 시도에 실패해도 재시도로 성공하면 데이터가 포함된다")
    void beforeStep_pageFetchFailsOnce_retriesAndSucceeds() throws Exception {
        // given
        TmdbMovieDto movie = new TmdbMovieDto(1L, "영화", "Movie", "desc", null, List.of(), "2024-01-01", 7.0, "ko");
        given(tmdbApiClient.fetchMovies(1))
                .willThrow(new RuntimeException("일시적 오류"))
                .willReturn(new TmdbMovieListResponse(1, List.of(movie), 1, 1));

        // when
        reader.beforeStep(createStepExecution("1", "1"));

        // then
        assertThat(reader.read()).isEqualTo(movie);
    }

    @Test
    @DisplayName("한 페이지가 재시도까지 모두 실패해도 다른 페이지 데이터는 유지된다")
    void beforeStep_pageFetchFailsAfterRetries_otherPagesPreserved() throws Exception {
        // given
        TmdbMovieDto movie = new TmdbMovieDto(2L, "영화2", "Movie2", "desc", null, List.of(), "2024-01-01", 7.0, "ko");
        given(tmdbApiClient.fetchMovies(1)).willThrow(new RuntimeException("지속 오류"));
        given(tmdbApiClient.fetchMovies(2)).willReturn(new TmdbMovieListResponse(2, List.of(movie), 2, 1));

        // when
        reader.beforeStep(createStepExecution("1", "2"));

        // then
        assertThat(reader.read()).isEqualTo(movie);
        assertThat(reader.read()).isNull();
    }
}
