package com.codeit.team5.mopl.content.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.codeit.team5.mopl.content.batch.dto.ContentWithMetaData;
import com.codeit.team5.mopl.content.dto.external.tmdb.TmdbTvDto;
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
class TmdbTvSeriesItemProcessorTest {

    @Mock
    private ContentRepository contentRepository;

    private TmdbTvSeriesItemProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new TmdbTvSeriesItemProcessor(contentRepository, new ObjectMapper());
    }

    @Test
    @DisplayName("이미 존재하는 TV 시리즈면 null을 반환한다")
    void process_duplicate_returnsNull() throws Exception {
        TmdbTvDto dto = new TmdbTvDto(1L, "브레이킹 배드", "Breaking Bad", "overview",
                "/poster.jpg", List.of(18L), "2008-01-20", 9.5, "en");
        given(contentRepository.existsBySourceAndExternalId(ContentSource.TMDB, "1")).willReturn(true);

        ContentWithMetaData result = processor.process(dto);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("새로운 TV 시리즈면 ContentWithMetaData를 반환한다")
    void process_newTvSeries_returnsContentWithMetaData() throws Exception {
        TmdbTvDto dto = new TmdbTvDto(1L, "브레이킹 배드", "Breaking Bad", "overview",
                "/poster.jpg", List.of(18L), "2008-01-20", 9.5, "en");
        given(contentRepository.existsBySourceAndExternalId(ContentSource.TMDB, "1")).willReturn(false);

        ContentWithMetaData result = processor.process(dto);

        assertThat(result).isNotNull();
        assertThat(result.content().getTitle()).isEqualTo("브레이킹 배드");
        assertThat(result.content().getType()).isEqualTo(ContentType.TV_SERIES);
        assertThat(result.content().getSource()).isEqualTo(ContentSource.TMDB);
        assertThat(result.thumbnailUrl()).isEqualTo("https://image.tmdb.org/t/p/w500/poster.jpg");
    }

    @Test
    @DisplayName("posterPath가 null이면 thumbnailUrl도 null이다")
    void process_nullPosterPath_thumbnailUrlIsNull() throws Exception {
        TmdbTvDto dto = new TmdbTvDto(2L, "포스터없는시리즈", "No Poster", "overview",
                null, List.of(), "2020-01-01", 7.0, "ko");
        given(contentRepository.existsBySourceAndExternalId(ContentSource.TMDB, "2")).willReturn(false);

        ContentWithMetaData result = processor.process(dto);

        assertThat(result).isNotNull();
        assertThat(result.thumbnailUrl()).isNull();
    }
}
