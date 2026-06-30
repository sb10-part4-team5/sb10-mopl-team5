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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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

        if (response.events() == null) {
            log.warn("[SportsDB] 수집된 경기 없음 - leagueId={}, season={}", leagueId, season);
            return;
        }

        transactionTemplate.execute(status -> {
            response.events().forEach(this::saveEventIfAbsent);
            return null;
        });
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
        String normalized = leagueName.trim().toLowerCase();
        Tag tag = tagRepository.findByName(normalized)
                .orElseGet(() -> tagRepository.save(Tag.create(normalized)));
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
        try {
            return objectMapper.writeValueAsString(
                    Map.of("sport", dto.strSport(), "league", dto.strLeague())
            );
        } catch (JsonProcessingException e) {
            log.warn("[SportsDB] metadata 직렬화 실패 - idEvent={}", dto.idEvent());
            return "{}";
        }
    }

    private Instant parseDate(String date) {
        if (!StringUtils.hasText(date)) {
            return null;
        }
        try {
            return LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (Exception e) {
            log.warn("[SportsDB] 날짜 파싱 실패 - date={}", date);
            return null;
        }
    }
}
