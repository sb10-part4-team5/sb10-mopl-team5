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
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.StringUtils;

@Slf4j
@RequiredArgsConstructor
public class TmdbMovieItemReader implements ItemReader<TmdbMovieDto> {

    private static final long REQUEST_DELAY_MS = 300;
    private static final int MAX_PAGE = 500;

    private final TmdbApiClient tmdbApiClient;
    private final RetryTemplate retryTemplate;

    private final Queue<TmdbMovieDto> buffer = new LinkedList<>();

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) throws InterruptedException {
        String startPageParam = stepExecution.getJobParameters().getString("startPage");
        String endPageParam = stepExecution.getJobParameters().getString("endPage");
        if (startPageParam == null || endPageParam == null) {
            throw new IllegalArgumentException("startPage, endPage 파라미터는 필수입니다.");
        }
        int currentPage = Integer.parseInt(startPageParam);
        int endPage = Math.min(Integer.parseInt(endPageParam), MAX_PAGE);
        buffer.clear();
        log.info("[TMDB] 영화 수집 범위 설정 - {}~{}페이지", currentPage, endPage);

        int totalPages = Integer.MAX_VALUE;
        while (currentPage <= Math.min(totalPages, endPage)) {
            int page = currentPage;
            try {
                TmdbMovieListResponse response = retryTemplate.execute(context ->
                        tmdbApiClient.fetchMovies(page));
                totalPages = response.totalPages();

                List<TmdbMovieDto> results = response.results();
                if (results.isEmpty()) {
                    log.info("[TMDB] 영화 {}페이지 응답이 비어 있어 수집을 종료합니다 - 전체 {}페이지", page, totalPages);
                    break;
                }

                results.stream()
                        .filter(dto -> StringUtils.hasText(dto.title()))
                        .forEach(buffer::add);
                log.info("[TMDB] 영화 {}페이지 로드 완료 - 누적 {}건 (전체 {}페이지)", page, buffer.size(), totalPages);
            } catch (Exception e) {
                // 재시도까지 소진된 페이지 하나 때문에 이미 모은 데이터까지 버리지 않도록,
                // 이 페이지만 건너뛰고 다음 페이지로 계속 진행한다.
                log.warn("[TMDB] 영화 {}페이지 조회 실패 (재시도 소진), 건너뜀 - error={}", page, e.getMessage());
            }
            currentPage++;
            Thread.sleep(REQUEST_DELAY_MS);
        }

        log.info("[TMDB] 영화 수집 완료 - 총 {}건", buffer.size());
    }

    @Override
    public TmdbMovieDto read() {
        return buffer.poll();
    }
}
