package com.codeit.team5.mopl.content.batch.reader;

import com.codeit.team5.mopl.content.client.tmdb.TmdbApiClient;
import com.codeit.team5.mopl.content.dto.external.tmdb.TmdbMovieDto;
import com.codeit.team5.mopl.content.dto.external.tmdb.TmdbMovieListResponse;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.util.StringUtils;

@Slf4j
@RequiredArgsConstructor
public class TmdbMovieItemReader implements ItemReader<TmdbMovieDto> {

    private static final long REQUEST_DELAY_MS = 300;
    private static final int MAX_PAGE = 500;

    private final TmdbApiClient tmdbApiClient;

    private int currentPage;
    private int endPage;
    private int totalPages = Integer.MAX_VALUE;
    private boolean noMoreData = false;
    private final Queue<TmdbMovieDto> buffer = new LinkedList<>();

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
        noMoreData = false;
        log.info("[TMDB] 영화 수집 범위 설정 - {}~{}페이지", currentPage, endPage);
    }

    @Override
    public TmdbMovieDto read() throws InterruptedException {
        while (buffer.isEmpty()) {
            if (noMoreData || currentPage > Math.min(totalPages, endPage)) {
                return null;
            }
            fetchNextPage();
        }
        return buffer.poll();
    }

    private void fetchNextPage() throws InterruptedException {
        TmdbMovieListResponse response = tmdbApiClient.fetchMovies(currentPage);
        totalPages = response.totalPages();

        List<TmdbMovieDto> results = response.results();
        if (results.isEmpty()) {
            log.info("[TMDB] 영화 {}페이지 응답이 비어 있어 수집을 종료합니다 - 전체 {}페이지", currentPage, totalPages);
            noMoreData = true;
            return;
        }

        results.stream()
                .filter(dto -> StringUtils.hasText(dto.title()))
                .forEach(buffer::add);

        log.info("[TMDB] 영화 {}페이지 로드 완료 - {}건 (전체 {}페이지)", currentPage, buffer.size(), totalPages);
        currentPage++;

        Thread.sleep(REQUEST_DELAY_MS);
    }
}
