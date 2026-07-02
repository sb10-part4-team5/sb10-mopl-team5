package com.codeit.team5.mopl.content.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.codeit.team5.mopl.content.batch.dto.ContentWithMetaData;
import com.codeit.team5.mopl.content.dto.external.sportsdb.SportsDbEventDto;
import com.codeit.team5.mopl.content.entity.ContentSource;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SportsDbEventItemProcessorTest {

    @Mock
    private ContentRepository contentRepository;

    private SportsDbEventItemProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new SportsDbEventItemProcessor(contentRepository, new ObjectMapper());
    }

    @Test
    @DisplayName("이미 존재하는 경기면 null을 반환한다")
    void process_duplicate_returnsNull() throws Exception {
        // given
        SportsDbEventDto dto = new SportsDbEventDto("123", "Arsenal vs Chelsea",
                "https://thumb.jpg", "2024-12-26", "English Premier League",
                "Soccer", "Arsenal", "Chelsea", "2", "1");
        given(contentRepository.existsBySourceAndExternalId(ContentSource.SPORTS_DB, "123")).willReturn(true);

        // when
        ContentWithMetaData result = processor.process(dto);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("새로운 경기면 ContentWithMetaData를 반환한다")
    void process_newEvent_returnsContentWithMetaData() throws Exception {
        // given
        SportsDbEventDto dto = new SportsDbEventDto("123", "Arsenal vs Chelsea",
                "https://thumb.jpg", "2024-12-26", "English Premier League",
                "Soccer", "Arsenal", "Chelsea", "2", "1");
        given(contentRepository.existsBySourceAndExternalId(ContentSource.SPORTS_DB, "123")).willReturn(false);

        // when
        ContentWithMetaData result = processor.process(dto);

        // then
        assertThat(result).isNotNull();
        assertThat(result.content().getTitle()).isEqualTo("Arsenal vs Chelsea");
        assertThat(result.content().getType()).isEqualTo(ContentType.SPORT);
        assertThat(result.content().getSource()).isEqualTo(ContentSource.SPORTS_DB);
        assertThat(result.thumbnailUrl()).isEqualTo("https://thumb.jpg");
    }

    @Test
    @DisplayName("리그명이 소문자로 정규화되어 tagNames에 포함된다")
    void process_leagueName_normalizedToLowerCase() throws Exception {
        // given
        SportsDbEventDto dto = new SportsDbEventDto("456", "Arsenal vs Chelsea",
                null, "2024-12-26", "English Premier League",
                "Soccer", "Arsenal", "Chelsea", null, null);
        given(contentRepository.existsBySourceAndExternalId(ContentSource.SPORTS_DB, "456")).willReturn(false);

        // when
        ContentWithMetaData result = processor.process(dto);

        // then
        assertThat(result).isNotNull();
        assertThat(result.tagNames()).containsExactly("english premier league");
    }

    @Test
    @DisplayName("스코어가 없으면 description에 vs가 포함된다")
    void process_noScore_descriptionContainsVs() throws Exception {
        // given
        SportsDbEventDto dto = new SportsDbEventDto("789", "Arsenal vs Chelsea",
                null, "2024-12-26", "English Premier League",
                "Soccer", "Arsenal", "Chelsea", null, null);
        given(contentRepository.existsBySourceAndExternalId(ContentSource.SPORTS_DB, "789")).willReturn(false);

        // when
        ContentWithMetaData result = processor.process(dto);

        // then
        assertThat(result).isNotNull();
        assertThat(result.content().getDescription()).contains("vs");
    }
}
