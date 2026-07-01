package com.codeit.team5.mopl.content.batch.reader;

import com.codeit.team5.mopl.content.client.sportsdb.SportsDbApiClient;
import com.codeit.team5.mopl.content.dto.external.sportsdb.SportsDbEventDto;
import com.codeit.team5.mopl.content.dto.external.sportsdb.SportsDbEventListResponse;
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
public class SportsDbEventItemReader implements ItemReader<SportsDbEventDto> {

    private final SportsDbApiClient sportsDbApiClient;

    private final Queue<SportsDbEventDto> buffer = new LinkedList<>();
    private boolean fetched = false;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        String leagueId = stepExecution.getJobParameters().getString("leagueId");
        String season = stepExecution.getJobParameters().getString("season");
        if (leagueId == null || season == null) {
            throw new IllegalArgumentException("leagueId, season 파라미터는 필수입니다.");
        }

        SportsDbEventListResponse response = sportsDbApiClient.fetchEventsBySeason(leagueId, season);

        if (response == null || response.events() == null) {
            log.warn("[SportsDB] 수집된 경기 없음 - leagueId={}, season={}", leagueId, season);
        } else {
            List<SportsDbEventDto> events = response.events();
            buffer.addAll(events);
            log.info("[SportsDB] leagueId={}, season={} - {}건 로드 완료", leagueId, season, events.size());
        }
        fetched = true;
    }

    @Override
    public SportsDbEventDto read() {
        if (!fetched) {
            return null;
        }
        return buffer.poll();
    }
}
