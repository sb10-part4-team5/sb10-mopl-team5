package com.codeit.team5.mopl.content.service;

import com.codeit.team5.mopl.binarycontent.repository.BinaryContentRepository;
import com.codeit.team5.mopl.content.service.util.ContentCollectionUtils;
import com.codeit.team5.mopl.content.client.tmdb.TmdbApiClient;
import com.codeit.team5.mopl.content.dto.external.tmdb.TmdbMovieDto;
import com.codeit.team5.mopl.content.dto.external.tmdb.TmdbMovieGenre;
import com.codeit.team5.mopl.content.dto.external.tmdb.TmdbMovieListResponse;
import com.codeit.team5.mopl.content.dto.external.tmdb.TmdbTvDto;
import com.codeit.team5.mopl.content.dto.external.tmdb.TmdbTvGenre;
import com.codeit.team5.mopl.content.dto.external.tmdb.TmdbTvListResponse;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentSource;
import com.codeit.team5.mopl.content.entity.ContentStats;
import com.codeit.team5.mopl.content.entity.ContentTag;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.content.repository.ContentStatsRepository;
import com.codeit.team5.mopl.tag.entity.Tag;
import com.codeit.team5.mopl.tag.repository.TagRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * TMDB API에서 영화·TV 시리즈 콘텐츠를 수집하여 DB에 저장하는 서비스.
 *
 * <p>수집 기준:
 * <ul>
 *   <li>한국어 번역 제목이 존재하는 콘텐츠만 저장 (title ≠ originalTitle 조건)</li>
 *   <li>성인 콘텐츠 제외: include_adult=false, 영화 certification ≤ R, TV certification ≤ TV-14</li>
 *   <li>중복 저장 방지: externalId 기준 존재 여부 확인</li>
 *   <li>페이지 단위 트랜잭션 적용, 요청 사이 {@value #REQUEST_DELAY_MS}ms 대기</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TmdbContentService {

    private static final String TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";
    private static final long REQUEST_DELAY_MS = 300;
    private static final int MAX_PAGE = 500;

    private final TmdbApiClient tmdbApiClient;
    private final ContentRepository contentRepository;
    private final ContentStatsRepository contentStatsRepository;
    private final BinaryContentRepository binaryContentRepository;
    private final TagRepository tagRepository;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    private record TmdbPageResult(List<TmdbContentData> items, int totalPages) {}

    private record TmdbContentData(
            ContentType type,
            String title,
            String externalId,
            String overview,
            String posterPath,
            List<String> tagNames,
            String releaseDate,
            double voteAverage,
            String originalLanguage
    ) {}

    /**
     * TMDB 인기 영화를 페이지 범위 단위로 수집한다.
     *
     * @param startPage 수집 시작 페이지 (1-based)
     * @param endPage   수집 종료 페이지 (최대 {@value #MAX_PAGE}로 자동 클램핑)
     */
    @Async("contentCollectionExecutor")
    public void collectMovies(int startPage, int endPage) {
        collectByPage("영화", startPage, endPage, page -> {
            TmdbMovieListResponse response = tmdbApiClient.fetchMovies(page);
            List<TmdbContentData> items = response.results().stream()
                    .filter(dto -> StringUtils.hasText(dto.title()))
                    .map(this::toContentData)
                    .toList();
            return new TmdbPageResult(items, response.totalPages());
        });
    }

    /**
     * TMDB 인기 TV 시리즈를 페이지 범위 단위로 수집한다.
     *
     * @param startPage 수집 시작 페이지 (1-based)
     * @param endPage   수집 종료 페이지 (최대 {@value #MAX_PAGE}로 자동 클램핑)
     */
    @Async("contentCollectionExecutor")
    public void collectTvSeries(int startPage, int endPage) {
        collectByPage("TV 시리즈", startPage, endPage, page -> {
            TmdbTvListResponse response = tmdbApiClient.fetchTvSeries(page);
            List<TmdbContentData> items = response.results().stream()
                    .filter(dto -> StringUtils.hasText(dto.name()))
                    .map(this::toContentData)
                    .toList();
            return new TmdbPageResult(items, response.totalPages());
        });
    }

    private void collectByPage(String label, int startPage, int endPage,
                                Function<Integer, TmdbPageResult> fetcher) {
        int clampedEnd = Math.min(endPage, MAX_PAGE);
        if (clampedEnd < endPage) {
            log.warn("[TMDB] endPage {}가 최대 허용 페이지({})를 초과하여 {}로 조정됩니다.", endPage, MAX_PAGE, clampedEnd);
        }
        for (int page = startPage; page <= clampedEnd; page++) {
            TmdbPageResult result = fetcher.apply(page);
            int actualLastPage = Math.min(result.totalPages(), clampedEnd);
            transactionTemplate.execute(status -> {
                result.items().forEach(this::saveIfAbsent);
                return null;
            });
            log.info("[TMDB] {} 수집 완료 - {}/{}페이지 ({}건)", label, page, result.totalPages(), result.items().size());
            if (page >= actualLastPage) {
                log.info("[TMDB] {} 수집 종료 - 실제 마지막 페이지({}) 도달", label, actualLastPage);
                break;
            }
            sleep();
        }
    }

    private void saveIfAbsent(TmdbContentData data) {
        if (contentRepository.existsBySourceAndExternalId(ContentSource.TMDB, data.externalId())) {
            log.debug("[TMDB] 스킵 (이미 존재) - externalId={}, title={}", data.externalId(), data.title());
            return;
        }

        Content content = contentRepository.save(
                Content.createByExternalSource(
                        data.type(),
                        data.title(),
                        data.overview(),
                        ContentSource.TMDB,
                        data.externalId(),
                        parseDate(data.releaseDate()),
                        buildMetadata(data.voteAverage(), data.originalLanguage())
                )
        );

        attachThumbnail(content, data.posterPath());
        attachTags(content, data.tagNames());
        contentStatsRepository.save(ContentStats.create(content));
    }

    private TmdbContentData toContentData(TmdbMovieDto dto) {
        List<String> tagNames = dto.genreIds().stream()
                .map(id -> TmdbMovieGenre.fromId(id).map(TmdbMovieGenre::getLabel))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();
        return new TmdbContentData(ContentType.MOVIE, dto.title(), String.valueOf(dto.id()),
                dto.overview(), dto.posterPath(), tagNames, dto.releaseDate(),
                dto.voteAverage(), dto.originalLanguage());
    }

    private TmdbContentData toContentData(TmdbTvDto dto) {
        List<String> tagNames = dto.genreIds().stream()
                .map(id -> TmdbTvGenre.fromId(id).map(TmdbTvGenre::getLabel))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();
        return new TmdbContentData(ContentType.TV_SERIES, dto.name(), String.valueOf(dto.id()),
                dto.overview(), dto.posterPath(), tagNames, dto.firstAirDate(),
                dto.voteAverage(), dto.originalLanguage());
    }

    private void attachThumbnail(Content content, String posterPath) {
        ContentCollectionUtils.attachThumbnail(content, posterPath, binaryContentRepository, TMDB_IMAGE_BASE_URL);
    }

    private void attachTags(Content content, List<String> tagNames) {
        if (tagNames.isEmpty()) {
            return;
        }
        // 소문자 키로 중복 제거하되, 저장 값은 원본 라벨 유지 (e.g. "SF" → key:"sf", label:"SF")
        Map<String, String> keyToLabel = new LinkedHashMap<>();
        tagNames.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(label -> keyToLabel.putIfAbsent(label.toLowerCase(), label));

        Map<String, Tag> existingTags = tagRepository.findByNameIn(List.copyOf(keyToLabel.values())).stream()
                .collect(Collectors.toMap(tag -> tag.getName().toLowerCase(), Function.identity()));

        List<Tag> newTags = keyToLabel.entrySet().stream()
                .filter(e -> !existingTags.containsKey(e.getKey()))
                .map(e -> Tag.create(e.getValue()))
                .toList();
        if (!newTags.isEmpty()) {
            tagRepository.saveAll(newTags).forEach(tag -> existingTags.put(tag.getName().toLowerCase(), tag));
        }

        keyToLabel.keySet().forEach(key -> content.addTag(ContentTag.create(content, existingTags.get(key))));
    }

    private Instant parseDate(String date) {
        return ContentCollectionUtils.parseDate(date, "TMDB");
    }

    private String buildMetadata(double voteAverage, String originalLanguage) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("voteAverage", voteAverage);
            metadata.put("originalLanguage", originalLanguage);
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("[TMDB] metadata 직렬화 실패 - voteAverage={}, originalLanguage={}", voteAverage, originalLanguage);
            return "{}";
        }
    }

    private void sleep() {
        try {
            Thread.sleep(REQUEST_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
