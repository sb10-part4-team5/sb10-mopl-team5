package com.codeit.team5.mopl.content.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.codeit.team5.mopl.content.batch.dto.ContentWithMetaData;
import com.codeit.team5.mopl.content.dto.external.tmdb.TmdbMovieDto;
import com.codeit.team5.mopl.content.entity.ContentSource;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TmdbMovieItemProcessorTest {

    @Mock
    private ContentRepository contentRepository;

    private TmdbMovieItemProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new TmdbMovieItemProcessor(contentRepository, new ObjectMapper());
    }

    @Test
    @DisplayName("이미 존재하는 영화면 null을 반환한다")
    void process_duplicate_returnsNull() throws Exception {
        // given
        TmdbMovieDto dto = new TmdbMovieDto(1L, "인터스텔라", "Interstellar", "overview",
                "/poster.jpg", List.of(18L), "2014-11-05", 8.6, "en");
        given(contentRepository.existsBySourceAndExternalId(ContentSource.TMDB, "1")).willReturn(true);

        // when
        ContentWithMetaData result = processor.process(dto);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("새로운 영화면 ContentWithMetaData를 반환한다")
    void process_newMovie_returnsContentWithMetaData() throws Exception {
        // given
        TmdbMovieDto dto = new TmdbMovieDto(1L, "인터스텔라", "Interstellar", "overview",
                "/poster.jpg", List.of(18L), "2014-11-05", 8.6, "en");
        given(contentRepository.existsBySourceAndExternalId(ContentSource.TMDB, "1")).willReturn(false);

        // when
        ContentWithMetaData result = processor.process(dto);

        // then
        assertThat(result).isNotNull();
        assertThat(result.content().getTitle()).isEqualTo("인터스텔라");
        assertThat(result.content().getType()).isEqualTo(ContentType.MOVIE);
        assertThat(result.content().getSource()).isEqualTo(ContentSource.TMDB);
        assertThat(result.thumbnailUrl()).isEqualTo("https://image.tmdb.org/t/p/w500/poster.jpg");
    }

    @Test
    @DisplayName("posterPath가 null이면 thumbnailUrl도 null이다")
    void process_nullPosterPath_thumbnailUrlIsNull() throws Exception {
        // given
        TmdbMovieDto dto = new TmdbMovieDto(2L, "포스터없는영화", "No Poster", "overview",
                null, List.of(), "2020-01-01", 7.0, "ko");
        given(contentRepository.existsBySourceAndExternalId(ContentSource.TMDB, "2")).willReturn(false);

        // when
        ContentWithMetaData result = processor.process(dto);

        // then
        assertThat(result).isNotNull();
        assertThat(result.thumbnailUrl()).isNull();
    }

    @Test
    @DisplayName("genreId가 매핑되면 tagNames에 포함된다")
    void process_validGenreId_tagNamesIncluded() throws Exception {
        // given
        TmdbMovieDto dto = new TmdbMovieDto(3L, "드라마영화", "Drama", "overview",
                null, List.of(18L), "2020-01-01", 7.0, "ko");
        given(contentRepository.existsBySourceAndExternalId(ContentSource.TMDB, "3")).willReturn(false);

        // when
        ContentWithMetaData result = processor.process(dto);

        // then
        assertThat(result).isNotNull();
        assertThat(result.tagNames()).containsExactly("드라마");
    }

    @Test
    @DisplayName("genreIds가 null이어도 예외 없이 tagNames가 빈 리스트로 처리된다")
    void process_nullGenreIds_tagNamesEmpty() throws Exception {
        // given
        TmdbMovieDto dto = new TmdbMovieDto(4L, "장르없는영화", "NoGenre", "overview",
                null, null, "2020-01-01", 7.0, "ko");
        given(contentRepository.existsBySourceAndExternalId(ContentSource.TMDB, "4")).willReturn(false);

        // when
        ContentWithMetaData result = processor.process(dto);

        // then
        assertThat(result).isNotNull();
        assertThat(result.tagNames()).isEmpty();
    }
}
