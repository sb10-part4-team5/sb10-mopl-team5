package com.codeit.team5.mopl.content.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.team5.mopl.content.batch.dto.ContentWithMetaData;
import com.codeit.team5.mopl.content.dto.external.sportsdb.SportsDbEventDto;
import com.codeit.team5.mopl.content.entity.ContentSource;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SportsDbEventItemProcessorTest {

    private SportsDbEventItemProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new SportsDbEventItemProcessor(new ObjectMapper());
    }

    @Test
    @DisplayName("경기 DTO를 ContentWithMetaData로 변환한다")
    void process_newEvent_returnsContentWithMetaData() throws Exception {
        // given
        SportsDbEventDto dto = new SportsDbEventDto("123", "Arsenal vs Chelsea",
                "https://thumb.jpg", "2024-12-26", "English Premier League",
                "Soccer", "Arsenal", "Chelsea", "2", "1");

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

        // when
        ContentWithMetaData result = processor.process(dto);

        // then
        assertThat(result).isNotNull();
        assertThat(result.content().getDescription()).contains("vs");
    }
}
