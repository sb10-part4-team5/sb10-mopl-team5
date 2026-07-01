package com.codeit.team5.mopl.content.batch.reader;

import com.codeit.team5.mopl.content.client.tmdb.TmdbApiClient;
import com.codeit.team5.mopl.content.dto.external.tmdb.TmdbTvDto;
import com.codeit.team5.mopl.content.dto.external.tmdb.TmdbTvListResponse;
import java.util.LinkedList;
import java.util.Queue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.util.StringUtils;

@Slf4j
@RequiredArgsConstructor
public class TmdbTvSeriesItemReader implements ItemReader<TmdbTvDto> {

    private static final long REQUEST_DELAY_MS = 300;
    private static final int MAX_PAGE = 500;

    private final TmdbApiClient tmdbApiClient;

    private int currentPage;
    private int endPage;
    private int totalPages = Integer.MAX_VALUE;
    private final Queue<TmdbTvDto> buffer = new LinkedList<>();

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        String startPageParam = stepExecution.getJobParameters().getString("startPage");
        String endPageParam = stepExecution.getJobParameters().getString("endPage");
        if (startPageParam == null || endPageParam == null) {
            throw new IllegalArgumentException("startPage, endPage 파라미터는 필수입니다.");
        }
        currentPage = Integer.parseInt(startPageParam);
        endPage = Math.min(Integer.parseInt(endPageParam), MAX_PAGE);
        buffer.clear();
        totalPages = Integer.MAX_VALUE;
        log.info("[TMDB] TV 시리즈 수집 범위 설정 - {}~{}페이지", currentPage, endPage);
    }

    @Override
    public TmdbTvDto read() throws InterruptedException {
        while (buffer.isEmpty()) {
            if (currentPage > Math.min(totalPages, endPage)) {
                return null;
            }
            fetchNextPage();
        }
        return buffer.poll();
    }

    private void fetchNextPage() throws InterruptedException {
        TmdbTvListResponse response = tmdbApiClient.fetchTvSeries(currentPage);
        totalPages = response.totalPages();

        response.results().stream()
                .filter(dto -> StringUtils.hasText(dto.name()))
                .forEach(buffer::add);

        log.info("[TMDB] TV 시리즈 {}페이지 로드 완료 - {}건 (전체 {}페이지)", currentPage, buffer.size(), totalPages);
        currentPage++;

        Thread.sleep(REQUEST_DELAY_MS);
    }
}
