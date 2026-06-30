package com.codeit.team5.mopl.content.service;

import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.binarycontent.repository.BinaryContentRepository;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    @Async("contentCollectionExecutor")
    @Transactional
    public void collectMovies(int startPage, int endPage) {
        int clampedEnd = Math.min(endPage, MAX_PAGE);
        if (clampedEnd < endPage) {
            log.warn("[TMDB] endPage {}가 최대 허용 페이지({})를 초과하여 {}로 조정됩니다.", endPage, MAX_PAGE, clampedEnd);
        }
        endPage = clampedEnd;
        for (int page = startPage; page <= endPage; page++) {
            TmdbMovieListResponse response = tmdbApiClient.fetchMovies(page);
            response.results().forEach(this::saveMovieIfAbsent);
            log.info("[TMDB] 영화 수집 완료 - {}/{}페이지 ({}건)", page, response.totalPages(), response.results().size());

            if (page < endPage) {
                sleep();
            }
        }
    }

    @Async("contentCollectionExecutor")
    @Transactional
    public void collectTvSeries(int startPage, int endPage) {
        int clampedEnd = Math.min(endPage, MAX_PAGE);
        if (clampedEnd < endPage) {
            log.warn("[TMDB] endPage {}가 최대 허용 페이지({})를 초과하여 {}로 조정됩니다.", endPage, MAX_PAGE, clampedEnd);
        }
        endPage = clampedEnd;
        for (int page = startPage; page <= endPage; page++) {
            TmdbTvListResponse response = tmdbApiClient.fetchTvSeries(page);
            response.results().forEach(this::saveTvSeriesIfAbsent);
            log.info("[TMDB] TV 시리즈 수집 완료 - {}/{}페이지 ({}건)", page, response.totalPages(), response.results().size());

            if (page < endPage) {
                sleep();
            }
        }
    }

    private void saveMovieIfAbsent(TmdbMovieDto dto) {
        if (dto.title().equals(dto.originalTitle())) {
            log.debug("[TMDB] 영화 스킵 (한국어 제목 없음) - externalId={}, title={}", dto.id(), dto.title());
            return;
        }
        String externalId = String.valueOf(dto.id());
        if (contentRepository.existsBySourceAndExternalId(ContentSource.TMDB, externalId)) {
            log.debug("[TMDB] 영화 스킵 (이미 존재) - externalId={}, title={}", externalId, dto.title());
            return;
        }

        String metadata = buildMetadata(dto.voteAverage(), dto.originalLanguage());
        Content content = contentRepository.save(
                Content.createByExternalSource(
                        ContentType.MOVIE,
                        dto.title(),
                        dto.overview(),
                        ContentSource.TMDB,
                        externalId,
                        parseDate(dto.releaseDate()),
                        metadata
                )
        );

        attachThumbnail(content, dto.posterPath());
        attachMovieTags(content, dto.genreIds());
        contentStatsRepository.save(ContentStats.create(content));
    }

    private void saveTvSeriesIfAbsent(TmdbTvDto dto) {
        if (dto.name().equals(dto.originalName())) {
            log.debug("[TMDB] TV 시리즈 스킵 (한국어 제목 없음) - externalId={}, name={}", dto.id(), dto.name());
            return;
        }
        String externalId = String.valueOf(dto.id());
        if (contentRepository.existsBySourceAndExternalId(ContentSource.TMDB, externalId)) {
            log.debug("[TMDB] TV 시리즈 스킵 (이미 존재) - externalId={}, name={}", externalId, dto.name());
            return;
        }

        String metadata = buildMetadata(dto.voteAverage(), dto.originalLanguage());
        Content content = contentRepository.save(
                Content.createByExternalSource(
                        ContentType.TV_SERIES,
                        dto.name(),
                        dto.overview(),
                        ContentSource.TMDB,
                        externalId,
                        parseDate(dto.firstAirDate()),
                        metadata
                )
        );

        attachThumbnail(content, dto.posterPath());
        attachTvTags(content, dto.genreIds());
        contentStatsRepository.save(ContentStats.create(content));
    }

    private void attachThumbnail(Content content, String posterPath) {
        if (!StringUtils.hasText(posterPath)) {
            return;
        }
        BinaryContent thumbnail = binaryContentRepository.save(
                BinaryContent.externalUrl(TMDB_IMAGE_BASE_URL + posterPath)
        );
        content.attachThumbnail(thumbnail);
    }

    private void attachMovieTags(Content content, List<Long> genreIds) {
        List<String> genreNames = genreIds.stream()
                .map(id -> TmdbMovieGenre.fromId(id).map(TmdbMovieGenre::getLabel))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();

        attachTags(content, genreNames);
    }

    private void attachTvTags(Content content, List<Long> genreIds) {
        List<String> genreNames = genreIds.stream()
                .map(id -> TmdbTvGenre.fromId(id).map(TmdbTvGenre::getLabel))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();

        attachTags(content, genreNames);
    }

    private void attachTags(Content content, List<String> tagNames) {
        if (tagNames.isEmpty()) {
            return;
        }

        Map<String, Tag> existingTags = tagRepository.findByNameIn(tagNames).stream()
                .collect(Collectors.toMap(Tag::getName, Function.identity()));

        tagNames.stream()
                .filter(name -> !existingTags.containsKey(name))
                .map(Tag::create)
                .forEach(tag -> existingTags.put(tag.getName(), tagRepository.save(tag)));

        tagNames.forEach(name -> content.addTag(ContentTag.create(content, existingTags.get(name))));
    }

    private Instant parseDate(String date) {
        if (!StringUtils.hasText(date)) {
            return null;
        }
        try {
            return LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (Exception e) {
            return null;
        }
    }

    private String buildMetadata(double voteAverage, String originalLanguage) {
        return String.format("{\"voteAverage\": %.1f, \"originalLanguage\": \"%s\"}",
                voteAverage, originalLanguage);
    }

    private void sleep() {
        try {
            Thread.sleep(REQUEST_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
