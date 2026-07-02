package com.codeit.team5.mopl.content.batch.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.content.client.tmdb.TmdbApiClient;
import com.codeit.team5.mopl.content.dto.external.tmdb.TmdbTvDto;
import com.codeit.team5.mopl.content.dto.external.tmdb.TmdbTvListResponse;
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

@ExtendWith(MockitoExtension.class)
class TmdbTvSeriesItemReaderTest {

    @Mock
    private TmdbApiClient tmdbApiClient;

    private TmdbTvSeriesItemReader reader;

    @BeforeEach
    void setUp() {
        reader = new TmdbTvSeriesItemReader(tmdbApiClient);
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
        TmdbTvDto tv1 = new TmdbTvDto(1L, "시리즈1", "Series1", "desc", null, List.of(), "2024-01-01", 7.0, "ko");
        TmdbTvDto tv2 = new TmdbTvDto(2L, "시리즈2", "Series2", "desc", null, List.of(), "2024-01-01", 7.0, "ko");
        given(tmdbApiClient.fetchTvSeries(1)).willReturn(new TmdbTvListResponse(1, List.of(tv1, tv2), 1, 2));
        reader.beforeStep(createStepExecution("1", "1"));

        // when, then
        assertThat(reader.read()).isEqualTo(tv1);
        assertThat(reader.read()).isEqualTo(tv2);
        assertThat(reader.read()).isNull();
    }

    @Test
    @DisplayName("name이 빈 시리즈는 필터링된다")
    void read_emptyName_filtered() throws Exception {
        // given
        TmdbTvDto validTv = new TmdbTvDto(1L, "시리즈1", "Series1", "desc", null, List.of(), "2024-01-01", 7.0, "ko");
        TmdbTvDto emptyName = new TmdbTvDto(2L, "", "NoName", "desc", null, List.of(), "2024-01-01", 7.0, "ko");
        given(tmdbApiClient.fetchTvSeries(1)).willReturn(new TmdbTvListResponse(1, List.of(validTv, emptyName), 1, 2));
        reader.beforeStep(createStepExecution("1", "1"));

        // when, then
        assertThat(reader.read()).isEqualTo(validTv);
        assertThat(reader.read()).isNull();
    }

    @Test
    @DisplayName("endPage가 MAX_PAGE(500)를 초과하면 500으로 클램핑되어 501페이지부터는 API를 호출하지 않는다")
    void beforeStep_endPageExceedsMax_clampsTo500() throws Exception {
        // given
        // startPage를 501로 시작시켜, 클램핑된 endPage(500)를 즉시 넘어서는지로 검증한다.
        // 클램핑이 없다면(endPage=9999 그대로) fetchTvSeries(501)이 호출되어 스텁되지 않은 응답으로 NPE가 발생한다.

        // when, then
        reader.beforeStep(createStepExecution("501", "9999"));

        assertThat(reader.read()).isNull();
        verify(tmdbApiClient, never()).fetchTvSeries(anyInt());
    }

    @Test
    @DisplayName("응답 results가 비어있으면 totalPages와 무관하게 즉시 종료하고 이후 페이지는 호출하지 않는다")
    void read_emptyResults_stopsImmediatelyWithoutCallingLaterPages() throws Exception {
        // given
        given(tmdbApiClient.fetchTvSeries(1)).willReturn(new TmdbTvListResponse(1, List.of(), 500, 0));

        // when, then
        reader.beforeStep(createStepExecution("1", "500"));

        assertThat(reader.read()).isNull();
        verify(tmdbApiClient, never()).fetchTvSeries(2);
    }
}
