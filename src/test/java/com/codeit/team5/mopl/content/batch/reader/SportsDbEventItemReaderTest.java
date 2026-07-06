package com.codeit.team5.mopl.content.batch.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
class SportsDbEventItemReaderTest {

    @Mock
    private SportsDbApiClient sportsDbApiClient;

    private SportsDbEventItemReader reader;

    @BeforeEach
    void setUp() {
        // 운영 설정(BatchConfig)과 동일하게 최대 2회 시도하되, 테스트 속도를 위해
        // 백오프(운영은 200ms)는 두지 않는다.
        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(2)
                .noBackoff()
                .retryOn(Exception.class)
                .build();
        reader = new SportsDbEventItemReader(sportsDbApiClient, retryTemplate);
    }

    private StepExecution createStepExecution(String leagueId, String season) {
        return MetaDataInstanceFactory.createStepExecution(
                new JobParametersBuilder()
                        .addString("leagueId", leagueId)
                        .addString("season", season)
                        .toJobParameters()
        );
    }

    @Test
    @DisplayName("leagueId, season 파라미터가 없으면 예외가 발생한다")
    void beforeStep_missingParams_throwsException() {
        // given
        StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution();

        // when, then
        assertThatThrownBy(() -> reader.beforeStep(stepExecution))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("경기가 있으면 버퍼에 적재되어 read()로 반환된다")
    void read_eventsExist_returnsEvents() {
        // given
        SportsDbEventDto event = new SportsDbEventDto("1", "Arsenal vs Chelsea",
                null, "2023-08-12", "English Premier League",
                "Soccer", "Arsenal", "Chelsea", "2", "1");
        given(sportsDbApiClient.fetchEventsBySeason("4328", "2023-2024"))
                .willReturn(new SportsDbEventListResponse(List.of(event)));

        // when
        reader.beforeStep(createStepExecution("4328", "2023-2024"));

        // then
        assertThat(reader.read()).isEqualTo(event);
        assertThat(reader.read()).isNull();
    }

    @Test
    @DisplayName("경기가 없으면 read()가 null을 반환한다")
    void read_noEvents_returnsNull() {
        // given
        given(sportsDbApiClient.fetchEventsBySeason("4328", "2023-2024")).willReturn(null);

        // when
        reader.beforeStep(createStepExecution("4328", "2023-2024"));

        // then
        assertThat(reader.read()).isNull();
    }

    @Test
    @DisplayName("조회가 첫 시도에 실패해도 재시도로 성공하면 데이터가 포함된다")
    void beforeStep_fetchFailsOnce_retriesAndSucceeds() {
        // given
        SportsDbEventDto event = new SportsDbEventDto("1", "Arsenal vs Chelsea",
                null, "2023-08-12", "English Premier League",
                "Soccer", "Arsenal", "Chelsea", "2", "1");
        given(sportsDbApiClient.fetchEventsBySeason("4328", "2023-2024"))
                .willThrow(new RuntimeException("일시적 오류"))
                .willReturn(new SportsDbEventListResponse(List.of(event)));

        // when
        reader.beforeStep(createStepExecution("4328", "2023-2024"));

        // then
        assertThat(reader.read()).isEqualTo(event);
    }

    @Test
    @DisplayName("재시도까지 모두 실패하면 beforeStep이 예외를 전파한다")
    void beforeStep_fetchFailsAfterRetries_propagatesException() {
        // given
        given(sportsDbApiClient.fetchEventsBySeason("4328", "2023-2024"))
                .willThrow(new RuntimeException("지속 오류"));

        // when, then
        assertThatThrownBy(() -> reader.beforeStep(createStepExecution("4328", "2023-2024")))
                .isInstanceOf(RuntimeException.class);
    }
}
