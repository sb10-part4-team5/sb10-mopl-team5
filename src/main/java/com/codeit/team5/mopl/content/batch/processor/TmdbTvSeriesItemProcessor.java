package com.codeit.team5.mopl.content.batch.processor;

import com.codeit.team5.mopl.content.batch.dto.ContentWithMetaData;
import com.codeit.team5.mopl.content.dto.external.tmdb.TmdbTvDto;
import com.codeit.team5.mopl.content.dto.external.tmdb.TmdbTvGenre;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentSource;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.content.service.util.ContentCollectionUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

@Slf4j
@RequiredArgsConstructor
public class TmdbTvSeriesItemProcessor implements ItemProcessor<TmdbTvDto, ContentWithMetaData> {

    private static final String TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";

    private final ObjectMapper objectMapper;

    @Override
    public ContentWithMetaData process(TmdbTvDto dto) {
        Content content = Content.createByExternalSource(
                ContentType.TV_SERIES,
                dto.name(),
                dto.overview(),
                ContentSource.TMDB,
                String.valueOf(dto.id()),
                ContentCollectionUtils.parseDate(dto.firstAirDate(), "TMDB"),
                buildMetadata(dto)
        );

        String thumbnailUrl = dto.posterPath() != null ? TMDB_IMAGE_BASE_URL + dto.posterPath() : null;
        List<String> tagNames = resolveTagNames(dto);

        return new ContentWithMetaData(content, thumbnailUrl, tagNames);
    }

    private List<String> resolveTagNames(TmdbTvDto dto) {
        return dto.genreIds().stream()
                .map(id -> TmdbTvGenre.fromId(id).map(TmdbTvGenre::getLabel))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();
    }

    private String buildMetadata(TmdbTvDto dto) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("voteAverage", dto.voteAverage());
            metadata.put("originalLanguage", dto.originalLanguage());
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("[TMDB] metadata 직렬화 실패 - externalId={}", dto.id());
            return "{}";
        }
    }
}
