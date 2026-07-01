package com.codeit.team5.mopl.content.batch.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.codeit.team5.mopl.content.client.sportsdb.SportsDbApiClient;
import com.codeit.team5.mopl.content.dto.external.sportsdb.SportsDbEventDto;
import com.codeit.team5.mopl.content.dto.external.sportsdb.SportsDbEventListResponse;
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
class SportsDbDayItemReaderTest {

    @Mock
    private SportsDbApiClient sportsDbApiClient;

    private SportsDbDayItemReader reader;

    @BeforeEach
    void setUp() {
        reader = new SportsDbDayItemReader(sportsDbApiClient);
    }

    private StepExecution createStepExecution(String date) {
        return MetaDataInstanceFactory.createStepExecution(
                new JobParametersBuilder()
                        .addString("date", date)
                        .toJobParameters()
        );
    }

    @Test
    @DisplayName("date 파라미터가 없으면 예외가 발생한다")
    void beforeStep_missingDate_throwsException() {
        StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution();

        assertThatThrownBy(() -> reader.beforeStep(stepExecution))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("경기가 있는 날짜면 버퍼에 적재되어 read()로 반환된다")
    void read_eventsExist_returnsEvents() {
        SportsDbEventDto event = new SportsDbEventDto("1", "Arsenal vs Chelsea",
                null, "2024-12-26", "English Premier League",
                "Soccer", "Arsenal", "Chelsea", "2", "1");
        given(sportsDbApiClient.fetchEventsByDay(anyString(), eq("2024-12-26")))
                .willReturn(new SportsDbEventListResponse(List.of(event)))
                .willReturn(null);

        reader.beforeStep(createStepExecution("2024-12-26"));

        assertThat(reader.read()).isEqualTo(event);
    }

    @Test
    @DisplayName("경기가 없는 날짜면 read()가 null을 반환한다")
    void read_noEvents_returnsNull() {
        given(sportsDbApiClient.fetchEventsByDay(anyString(), eq("2024-07-01")))
                .willReturn(null);

        reader.beforeStep(createStepExecution("2024-07-01"));

        assertThat(reader.read()).isNull();
    }
}
