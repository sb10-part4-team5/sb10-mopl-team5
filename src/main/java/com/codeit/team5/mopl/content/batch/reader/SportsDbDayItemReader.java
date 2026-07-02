package com.codeit.team5.mopl.content.batch.reader;

import com.codeit.team5.mopl.content.client.sportsdb.SportsDbApiClient;
import com.codeit.team5.mopl.content.dto.external.sportsdb.SportsDbEventDto;
import com.codeit.team5.mopl.content.dto.external.sportsdb.SportsDbEventListResponse;
import com.codeit.team5.mopl.content.dto.external.sportsdb.SportsDbLeague;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.retry.support.RetryTemplate;

@Slf4j
@RequiredArgsConstructor
public class SportsDbDayItemReader implements ItemReader<SportsDbEventDto> {

    private final SportsDbApiClient sportsDbApiClient;
    private final RetryTemplate retryTemplate;

    private final Queue<SportsDbEventDto> buffer = new LinkedList<>();

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        String date = stepExecution.getJobParameters().getString("date");
        if (date == null) {
            throw new IllegalArgumentException("date 파라미터는 필수입니다.");
        }

        for (SportsDbLeague league : SportsDbLeague.values()) {
            fetchLeagueEvents(league, date);
        }

        log.info("[SportsDB] 일별 수집 완료 - 총 {}건", buffer.size());
    }

    // 리그 하나의 조회 실패(네트워크 오류 등)가 전체 Step 실패로 번져 이미 수집한
    // 다른 리그 데이터까지 유실되지 않도록, 재시도 후에도 실패하면 해당 리그만 건너뛴다.
    private void fetchLeagueEvents(SportsDbLeague league, String date) {
        try {
            SportsDbEventListResponse response = retryTemplate.execute(context ->
                    sportsDbApiClient.fetchEventsByDay(league.getLeagueId(), date));

            if (response == null || response.events() == null) {
                log.debug("[SportsDB] 경기 없음 - league={}, date={}", league.getName(), date);
                return;
            }
            List<SportsDbEventDto> events = response.events();
            buffer.addAll(events);
            log.info("[SportsDB] league={}, date={} - {}건 로드", league.getName(), date, events.size());
        } catch (Exception e) {
            log.warn("[SportsDB] league={} 조회 실패 (재시도 소진), 건너뜀 - date={}, error={}",
                    league.getName(), date, e.getMessage());
        }
    }

    @Override
    public SportsDbEventDto read() {
        return buffer.poll();
    }
}
