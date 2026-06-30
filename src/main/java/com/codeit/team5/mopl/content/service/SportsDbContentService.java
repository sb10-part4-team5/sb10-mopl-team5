package com.codeit.team5.mopl.content.service;

import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.binarycontent.repository.BinaryContentRepository;
import com.codeit.team5.mopl.content.client.sportsdb.SportsDbApiClient;
import com.codeit.team5.mopl.content.dto.external.sportsdb.SportsDbEventDto;
import com.codeit.team5.mopl.content.dto.external.sportsdb.SportsDbEventListResponse;
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
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class SportsDbContentService {

    private final SportsDbApiClient sportsDbApiClient;
    private final ContentRepository contentRepository;
    private final ContentStatsRepository contentStatsRepository;
    private final BinaryContentRepository binaryContentRepository;
    private final TagRepository tagRepository;

    @Async("contentCollectionExecutor")
    @Transactional
    public void collectEvents(String leagueId, String season) {
        SportsDbEventListResponse response = sportsDbApiClient.fetchEventsBySeason(leagueId, season);

        if (response.events() == null) {
            log.warn("[SportsDB] 수집된 경기 없음 - leagueId={}, season={}", leagueId, season);
            return;
        }

        response.events().forEach(this::saveEventIfAbsent);
        log.info("[SportsDB] 경기 수집 완료 - leagueId={}, season={}, {}건", leagueId, season, response.events().size());
    }

    private void saveEventIfAbsent(SportsDbEventDto dto) {
        if (contentRepository.existsBySourceAndExternalId(ContentSource.SPORTS_DB, dto.idEvent())) {
            log.debug("[SportsDB] 경기 스킵 (이미 존재) - idEvent={}, event={}", dto.idEvent(), dto.strEvent());
            return;
        }

        Content content = contentRepository.save(
                Content.createByExternalSource(
                        ContentType.SPORT,
                        dto.strEvent(),
                        buildDescription(dto),
                        ContentSource.SPORTS_DB,
                        dto.idEvent(),
                        parseDate(dto.dateEvent()),
                        buildMetadata(dto)
                )
        );

        attachThumbnail(content, dto.strThumb());
        attachLeagueTag(content, dto.strLeague());
        contentStatsRepository.save(ContentStats.create(content));
    }

    private void attachThumbnail(Content content, String thumbUrl) {
        if (!StringUtils.hasText(thumbUrl)) {
            return;
        }
        BinaryContent thumbnail = binaryContentRepository.save(
                BinaryContent.externalUrl(thumbUrl)
        );
        content.attachThumbnail(thumbnail);
    }

    private void attachLeagueTag(Content content, String leagueName) {
        if (!StringUtils.hasText(leagueName)) {
            return;
        }
        Tag tag = tagRepository.findByName(leagueName)
                .orElseGet(() -> tagRepository.save(Tag.create(leagueName)));
        content.addTag(ContentTag.create(content, tag));
    }

    private String buildDescription(SportsDbEventDto dto) {
        String score = hasScore(dto)
                ? dto.intHomeScore() + " - " + dto.intAwayScore()
                : "vs";
        return String.format("%s | %s %s %s", dto.strLeague(), dto.strHomeTeam(), score, dto.strAwayTeam());
    }

    private boolean hasScore(SportsDbEventDto dto) {
        return StringUtils.hasText(dto.intHomeScore()) && StringUtils.hasText(dto.intAwayScore());
    }

    private String buildMetadata(SportsDbEventDto dto) {
        return String.format("{\"sport\": \"%s\", \"league\": \"%s\"}", dto.strSport(), dto.strLeague());
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
}
