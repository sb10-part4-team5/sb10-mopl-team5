package com.codeit.team5.mopl.content.service;

import com.codeit.team5.mopl.binarycontent.repository.BinaryContentRepository;
import com.codeit.team5.mopl.content.service.util.ContentCollectionUtils;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * TheSportsDB API에서 스포츠 경기 이벤트를 수집하여 DB에 저장하는 서비스.
 *
 * <p>수집 기준:
 * <ul>
 *   <li>리그 ID와 시즌을 조합하여 한 시즌의 전체 경기를 일괄 수집</li>
 *   <li>중복 저장 방지: idEvent 기준 존재 여부 확인</li>
 *   <li>리그명을 정규화(소문자 trim)하여 태그로 부착</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SportsDbContentService {

    private final SportsDbApiClient sportsDbApiClient;
    private final ContentRepository contentRepository;
    private final ContentStatsRepository contentStatsRepository;
    private final BinaryContentRepository binaryContentRepository;
    private final TagRepository tagRepository;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 특정 리그의 한 시즌 전체 경기를 수집한다.
     *
     * @param leagueId TheSportsDB 리그 ID (예: "4328" = EPL)
     * @param season   시즌 문자열 (예: "2023-2024")
     */
    @Async("contentCollectionExecutor")
    public void collectEvents(String leagueId, String season) {
        SportsDbEventListResponse response = sportsDbApiClient.fetchEventsBySeason(leagueId, season);

        if (response == null || response.events() == null || response.events().isEmpty()) {
            log.warn("[SportsDB] 수집된 경기 없음 - leagueId={}, season={}", leagueId, season);
            return;
        }

        transactionTemplate.execute(status -> {
            Tag leagueTag = resolveLeagueTag(response.events().get(0).strLeague());
            response.events().forEach(dto -> saveEventIfAbsent(dto, leagueTag));
            return null;
        });
        log.info("[SportsDB] 경기 수집 완료 - leagueId={}, season={}, {}건", leagueId, season, response.events().size());
    }

    private void saveEventIfAbsent(SportsDbEventDto dto, Tag leagueTag) {
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
        if (leagueTag != null) {
            content.addTag(ContentTag.create(content, leagueTag));
        }
        contentStatsRepository.save(ContentStats.create(content));
    }

    private Tag resolveLeagueTag(String leagueName) {
        if (!StringUtils.hasText(leagueName)) {
            return null;
        }
        String normalized = leagueName.trim().toLowerCase();
        return tagRepository.findByName(normalized)
                .orElseGet(() -> tagRepository.save(Tag.create(normalized)));
    }

    private void attachThumbnail(Content content, String thumbUrl) {
        ContentCollectionUtils.attachThumbnail(content, thumbUrl, binaryContentRepository, "");
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

    private Instant parseDate(String date) {
        return ContentCollectionUtils.parseDate(date, "SportsDB");
    }
}
