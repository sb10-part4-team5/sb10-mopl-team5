package com.codeit.team5.mopl.content.batch.reader;

import com.codeit.team5.mopl.content.client.sportsdb.SportsDbApiClient;
import com.codeit.team5.mopl.content.dto.external.sportsdb.SportsDbEventDto;
import com.codeit.team5.mopl.content.dto.external.sportsdb.SportsDbEventListResponse;
import com.codeit.team5.mopl.content.dto.external.sportsdb.SportsDbLeague;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;

@Slf4j
@RequiredArgsConstructor
public class SportsDbDayItemReader implements ItemReader<SportsDbEventDto> {

    private final SportsDbApiClient sportsDbApiClient;

    private final Queue<SportsDbEventDto> buffer = new LinkedList<>();

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        String today = LocalDate.now().toString();

        for (SportsDbLeague league : SportsDbLeague.values()) {
            SportsDbEventListResponse response = sportsDbApiClient.fetchEventsByDay(league.getLeagueId(), today);
            if (response == null || response.events() == null) {
                log.debug("[SportsDB] 오늘 경기 없음 - league={}, date={}", league.getName(), today);
                continue;
            }
            List<SportsDbEventDto> events = response.events();
            buffer.addAll(events);
            log.info("[SportsDB] league={}, date={} - {}건 로드", league.getName(), today, events.size());
        }

        log.info("[SportsDB] 일별 수집 완료 - 총 {}건", buffer.size());
    }

    @Override
    public SportsDbEventDto read() {
        return buffer.poll();
    }
}
