package com.codeit.team5.mopl.content.batch.writer;

import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.binarycontent.repository.BinaryContentRepository;
import com.codeit.team5.mopl.content.batch.dto.ContentWithMetaData;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentSource;
import com.codeit.team5.mopl.content.entity.ContentStats;
import com.codeit.team5.mopl.content.entity.ContentTag;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.content.repository.ContentStatsRepository;
import com.codeit.team5.mopl.tag.entity.Tag;
import com.codeit.team5.mopl.tag.repository.TagRepository;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.util.StringUtils;

@Slf4j
@RequiredArgsConstructor
public class ContentItemWriter implements ItemWriter<ContentWithMetaData> {

    private final ContentRepository contentRepository;
    private final ContentStatsRepository contentStatsRepository;
    private final BinaryContentRepository binaryContentRepository;
    private final TagRepository tagRepository;

    @Override
    public void write(Chunk<? extends ContentWithMetaData> chunk) {
        List<? extends ContentWithMetaData> items = chunk.getItems();

        if (items.isEmpty()) {
            log.debug("[Batch] 빈 청크 수신 — 저장 생략");
            return;
        }

        // 1. 청크 내부 externalId 중복 제거
        // TMDB는 인기순 정렬 특성상 페이지 경계에서 같은 항목이 다음 페이지에 다시 나올 수 있어,
        // 한 청크 안에 동일 externalId가 들어올 수 있다. contents(source, external_id) 유니크 제약에
        // 걸려 청크 전체가 롤백되는 것을 막기 위해 저장 전에 externalId 기준으로 먼저 걸러낸다.
        // LinkedHashMap 병합 함수로 "먼저 온 항목 유지" 기준을 명시하고 원본 순서를 보존한다.
        Map<String, ContentWithMetaData> uniqueByExternalId = items.stream()
                .collect(Collectors.toMap(
                        item -> item.content().getExternalId(),
                        item -> (ContentWithMetaData) item,
                        (first, second) -> first,
                        LinkedHashMap::new
                ));
        Collection<ContentWithMetaData> uniqueItems = uniqueByExternalId.values();

        // 2. DB에 이미 존재하는 externalId 조회 후 신규 항목만 필터 (SELECT 1번)
        List<String> externalIds = uniqueByExternalId.keySet().stream().toList();
        ContentSource source = uniqueItems.iterator().next().content().getSource();
        Set<String> existingIds = contentRepository.findExternalIdsBySourceAndExternalIdIn(source, externalIds);

        List<ContentWithMetaData> deduplicatedItems = uniqueItems.stream()
                .filter(item -> !existingIds.contains(item.content().getExternalId()))
                .toList();

        if (deduplicatedItems.isEmpty()) {
            log.info("[Batch] 신규 항목 없음 — 저장 생략 (청크 원본: {}건)", items.size());
            return;
        }

        List<Content> contents = deduplicatedItems.stream()
                .map(ContentWithMetaData::content)
                .toList();
        contentRepository.saveAll(contents);

        // 2. ContentStats 일괄 저장
        List<ContentStats> stats = contents.stream()
                .map(ContentStats::create)
                .toList();
        contentStatsRepository.saveAll(stats);

        // 3. 썸네일 저장 (아이템별로 별도 BinaryContent를 저장 — thumbnail_id는 1:1 유니크 제약이라
        // 서로 다른 콘텐츠가 같은 thumbnailUrl을 갖더라도 BinaryContent를 공유할 수 없다.
        // saveAll은 입력 순서를 보존해 반환하므로 인덱스로 1:1 매칭한다.
        List<? extends ContentWithMetaData> itemsWithThumbnail = deduplicatedItems.stream()
                .filter(item -> StringUtils.hasText(item.thumbnailUrl()))
                .toList();
        if (!itemsWithThumbnail.isEmpty()) {
            List<BinaryContent> savedThumbnails = binaryContentRepository.saveAll(
                    itemsWithThumbnail.stream()
                            .map(item -> BinaryContent.completed(item.thumbnailUrl()))
                            .toList()
            );
            for (int i = 0; i < itemsWithThumbnail.size(); i++) {
                itemsWithThumbnail.get(i).content().attachThumbnail(savedThumbnails.get(i));
            }
        }

        // 4. 태그 저장
        List<String> allTagNames = normalizeTagNames(deduplicatedItems.stream()
                .flatMap(item -> item.tagNames().stream())
                .toList());

        if (!allTagNames.isEmpty()) {
            Map<String, Tag> existingTags = tagRepository.findOrCreateAllByName(allTagNames);

            deduplicatedItems.forEach(item -> item.tagNames().forEach(rawTagName -> {
                String normalized = normalizeTagName(rawTagName);
                Tag tag = normalized == null ? null : existingTags.get(normalized);
                if (tag != null) {
                    item.content().addTag(ContentTag.create(item.content(), tag));
                }
            }));
        }

        log.info("[Batch] {}건 저장 완료 (청크 원본: {}건)", deduplicatedItems.size(), items.size());
    }

    // 배치 수집 콘텐츠는 관리자 콘텐츠(ContentService/ContentTagService)와 도메인·검증 규칙이 달라
    // 태그 정규화/해석 로직을 공유하지 않고 이 클래스 안에서 자체적으로 처리한다.

    private String normalizeTagName(String rawName) {
        String trimmed = rawName.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase();
    }

    private List<String> normalizeTagNames(List<String> rawTagNames) {
        return rawTagNames.stream()
                .map(this::normalizeTagName)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
