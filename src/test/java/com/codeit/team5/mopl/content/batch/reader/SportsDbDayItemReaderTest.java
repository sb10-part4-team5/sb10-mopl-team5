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
import org.springframework.retry.support.RetryTemplate;

@ExtendWith(MockitoExtension.class)
class SportsDbDayItemReaderTest {

    @Mock
    private SportsDbApiClient sportsDbApiClient;

    private SportsDbDayItemReader reader;

    @BeforeEach
    void setUp() {
        // 운영 설정(BatchConfig)과 동일하게 최대 2회 시도하되, 테스트 속도를 위해
        // 백오프(운영은 200ms)는 두지 않는다.
        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(2)
                .noBackoff()
                .retryOn(Exception.class)
                .build();
        reader = new SportsDbDayItemReader(sportsDbApiClient, retryTemplate);
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
        // given
        StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution();

        // when, then
        assertThatThrownBy(() -> reader.beforeStep(stepExecution))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("경기가 있는 날짜면 버퍼에 적재되어 read()로 반환된다")
    void read_eventsExist_returnsEvents() {
        // given
        SportsDbEventDto event = new SportsDbEventDto("1", "Arsenal vs Chelsea",
                null, "2024-12-26", "English Premier League",
                "Soccer", "Arsenal", "Chelsea", "2", "1");
        given(sportsDbApiClient.fetchEventsByDay(anyString(), eq("2024-12-26")))
                .willReturn(new SportsDbEventListResponse(List.of(event)))
                .willReturn(null);
        reader.beforeStep(createStepExecution("2024-12-26"));

        // when, then
        assertThat(reader.read()).isEqualTo(event);
    }

    @Test
    @DisplayName("경기가 없는 날짜면 read()가 null을 반환한다")
    void read_noEvents_returnsNull() {
        // given
        given(sportsDbApiClient.fetchEventsByDay(anyString(), eq("2024-07-01")))
                .willReturn(null);
        reader.beforeStep(createStepExecution("2024-07-01"));

        // when, then
        assertThat(reader.read()).isNull();
    }

    @Test
    @DisplayName("리그 조회가 첫 시도에 실패해도 재시도로 성공하면 데이터가 포함된다")
    void read_leagueFetchFailsOnce_retriesAndSucceeds() {
        // given
        SportsDbEventDto event = new SportsDbEventDto("1", "Arsenal vs Chelsea",
                null, "2024-12-26", "English Premier League",
                "Soccer", "Arsenal", "Chelsea", "2", "1");
        given(sportsDbApiClient.fetchEventsByDay(eq("4328"), eq("2024-12-26")))
                .willThrow(new RuntimeException("일시적 오류"))
                .willReturn(new SportsDbEventListResponse(List.of(event)));
        reader.beforeStep(createStepExecution("2024-12-26"));

        // when, then
        assertThat(reader.read()).isEqualTo(event);
    }

    @Test
    @DisplayName("한 리그가 재시도까지 모두 실패해도 다른 리그 데이터는 유지된다")
    void read_leagueFetchFailsAfterRetries_otherLeaguesPreserved() {
        // given
        SportsDbEventDto event = new SportsDbEventDto("2", "Real Madrid vs Barcelona",
                null, "2024-12-26", "Spanish La Liga",
                "Soccer", "Real Madrid", "Barcelona", "3", "1");
        // EPL(leagueId=4328)은 재시도까지 계속 실패한다.
        given(sportsDbApiClient.fetchEventsByDay(eq("4328"), eq("2024-12-26")))
                .willThrow(new RuntimeException("지속 오류"));
        // LA_LIGA(leagueId=4335)는 정상 응답한다.
        given(sportsDbApiClient.fetchEventsByDay(eq("4335"), eq("2024-12-26")))
                .willReturn(new SportsDbEventListResponse(List.of(event)));
        reader.beforeStep(createStepExecution("2024-12-26"));

        // when, then
        assertThat(reader.read()).isEqualTo(event);
        assertThat(reader.read()).isNull();
    }
}
