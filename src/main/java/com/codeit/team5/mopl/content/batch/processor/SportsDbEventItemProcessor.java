package com.codeit.team5.mopl.content.batch.processor;

import com.codeit.team5.mopl.content.batch.dto.ContentWithMetaData;
import com.codeit.team5.mopl.content.dto.external.sportsdb.SportsDbEventDto;
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
import org.springframework.util.StringUtils;

@Slf4j
@RequiredArgsConstructor
public class SportsDbEventItemProcessor implements ItemProcessor<SportsDbEventDto, ContentWithMetaData> {

    private final ObjectMapper objectMapper;

    @Override
    public ContentWithMetaData process(SportsDbEventDto dto) {
        Content content = Content.createByExternalSource(
                ContentType.SPORT,
                dto.strEvent(),
                buildDescription(dto),
                ContentSource.SPORTS_DB,
                dto.idEvent(),
                ContentCollectionUtils.parseDate(dto.dateEvent(), "SportsDB"),
                buildMetadata(dto)
        );

        String leagueName = StringUtils.hasText(dto.strLeague())
                ? dto.strLeague().trim().toLowerCase()
                : null;

        return new ContentWithMetaData(content, dto.strThumb(), leagueName != null ? List.of(leagueName) : List.of());
    }

    private String buildDescription(SportsDbEventDto dto) {
        String score = StringUtils.hasText(dto.intHomeScore()) && StringUtils.hasText(dto.intAwayScore())
                ? dto.intHomeScore() + " - " + dto.intAwayScore()
                : "vs";
        return String.format("%s | %s %s %s", dto.strLeague(), dto.strHomeTeam(), score, dto.strAwayTeam());
    }

    private String buildMetadata(SportsDbEventDto dto) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("sport", dto.strSport());
            metadata.put("league", dto.strLeague());
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("[SportsDB] metadata 직렬화 실패 - idEvent={}", dto.idEvent());
            return "{}";
        }
    }
}
