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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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
        List<ContentWithMetaData> items = List.copyOf(chunk.getItems());

        // 1. DB에 이미 존재하는 externalId 조회 후 신규 항목만 필터 (SELECT 1번)
        List<String> externalIds = items.stream()
                .map(item -> item.content().getExternalId())
                .toList();
        ContentSource source = items.get(0).content().getSource();
        Set<String> existingIds = contentRepository.findExternalIdsBySourceAndExternalIdIn(source, externalIds);

        List<ContentWithMetaData> deduplicatedItems = items.stream()
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
        List<ContentWithMetaData> itemsWithThumbnail = deduplicatedItems.stream()
                .filter(item -> StringUtils.hasText(item.thumbnailUrl()))
                .toList();
        if (!itemsWithThumbnail.isEmpty()) {
            List<BinaryContent> savedThumbnails = binaryContentRepository.saveAll(
                    itemsWithThumbnail.stream()
                            .map(item -> BinaryContent.externalUrl(item.thumbnailUrl()))
                            .toList()
            );
            for (int i = 0; i < itemsWithThumbnail.size(); i++) {
                itemsWithThumbnail.get(i).content().attachThumbnail(savedThumbnails.get(i));
            }
        }

        // 4. 태그 저장
        List<String> allTagNames = deduplicatedItems.stream()
                .flatMap(item -> item.tagNames().stream())
                .distinct()
                .toList();

        if (!allTagNames.isEmpty()) {
            Map<String, Tag> existingTags = tagRepository.findByNameIn(allTagNames).stream()
                    .collect(Collectors.toMap(Tag::getName, Function.identity()));

            List<Tag> newTags = allTagNames.stream()
                    .filter(name -> !existingTags.containsKey(name))
                    .map(Tag::create)
                    .toList();

            if (!newTags.isEmpty()) {
                tagRepository.saveAll(newTags).forEach(tag -> existingTags.put(tag.getName(), tag));
            }

            deduplicatedItems.forEach(item -> item.tagNames().forEach(tagName ->
                    item.content().addTag(ContentTag.create(item.content(), existingTags.get(tagName)))
            ));
        }

        log.info("[Batch] {}건 저장 완료 (청크 원본: {}건)", deduplicatedItems.size(), items.size());
    }
}
