package com.codeit.team5.mopl.content.batch.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

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
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.MetaDataInstanceFactory;

@ExtendWith(MockitoExtension.class)
class TmdbMovieItemReaderTest {

    @Mock
    private TmdbApiClient tmdbApiClient;

    private TmdbMovieItemReader reader;

    @BeforeEach
    void setUp() {
        reader = new TmdbMovieItemReader(tmdbApiClient);
    }

    private StepExecution createStepExecution(String startPage, String endPage) {
        return MetaDataInstanceFactory.createStepExecution(
                new org.springframework.batch.core.JobParametersBuilder()
                        .addString("startPage", startPage)
                        .addString("endPage", endPage)
                        .toJobParameters()
        );
    }

    @Test
    @DisplayName("startPage, endPage 파라미터가 없으면 예외가 발생한다")
    void beforeStep_missingParams_throwsException() {
        StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution();

        assertThatThrownBy(() -> reader.beforeStep(stepExecution))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("1페이지 데이터를 순서대로 읽는다")
    void read_returnsItemsInOrder() throws Exception {
        TmdbMovieDto movie1 = new TmdbMovieDto(1L, "영화1", "Movie1", "desc", null, List.of(), "2024-01-01", 7.0, "ko");
        TmdbMovieDto movie2 = new TmdbMovieDto(2L, "영화2", "Movie2", "desc", null, List.of(), "2024-01-01", 7.0, "ko");
        given(tmdbApiClient.fetchMovies(1)).willReturn(new TmdbMovieListResponse(1, List.of(movie1, movie2), 1, 2));

        reader.beforeStep(createStepExecution("1", "1"));

        assertThat(reader.read()).isEqualTo(movie1);
        assertThat(reader.read()).isEqualTo(movie2);
        assertThat(reader.read()).isNull();
    }

    @Test
    @DisplayName("title이 빈 영화는 필터링된다")
    void read_emptyTitle_filtered() throws Exception {
        TmdbMovieDto validMovie = new TmdbMovieDto(1L, "영화1", "Movie1", "desc", null, List.of(), "2024-01-01", 7.0, "ko");
        TmdbMovieDto emptyTitle = new TmdbMovieDto(2L, "", "NoTitle", "desc", null, List.of(), "2024-01-01", 7.0, "ko");
        given(tmdbApiClient.fetchMovies(1)).willReturn(new TmdbMovieListResponse(1, List.of(validMovie, emptyTitle), 1, 2));

        reader.beforeStep(createStepExecution("1", "1"));

        assertThat(reader.read()).isEqualTo(validMovie);
        assertThat(reader.read()).isNull();
    }

    @Test
    @DisplayName("endPage가 MAX_PAGE(500)를 초과하면 500으로 클램핑된다")
    void beforeStep_endPageExceedsMax_clampsTo500() throws Exception {
        TmdbMovieDto movie = new TmdbMovieDto(1L, "영화", "Movie", "desc", null, List.of(), "2024-01-01", 7.0, "ko");
        given(tmdbApiClient.fetchMovies(1)).willReturn(new TmdbMovieListResponse(1, List.of(movie), 1, 1));

        reader.beforeStep(createStepExecution("1", "9999"));
        reader.read();

        assertThat(reader.read()).isNull();
    }
}
