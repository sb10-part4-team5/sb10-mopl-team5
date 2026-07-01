package com.codeit.team5.mopl.content.batch.writer;

import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.binarycontent.repository.BinaryContentRepository;
import com.codeit.team5.mopl.content.batch.dto.ContentWithMetaData;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentStats;
import com.codeit.team5.mopl.content.entity.ContentTag;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.content.repository.ContentStatsRepository;
import com.codeit.team5.mopl.tag.entity.Tag;
import com.codeit.team5.mopl.tag.repository.TagRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        List<ContentWithMetaData> items = new java.util.ArrayList<>(chunk.getItems());

        // 1. Content 일괄 저장 (청크 내 externalId 중복 제거 — TMDB가 페이지 간 동일 영화를 중복 반환하는 경우 대비)
        List<ContentWithMetaData> deduplicatedItems = items.stream()
                .collect(Collectors.toMap(
                        item -> item.content().getExternalId(),
                        Function.identity(),
                        (a, b) -> a,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .toList();

        List<Content> contents = deduplicatedItems.stream()
                .map(ContentWithMetaData::content)
                .toList();
        contentRepository.saveAll(contents);

        // 2. ContentStats 일괄 저장
        List<ContentStats> stats = contents.stream()
                .map(ContentStats::create)
                .toList();
        contentStatsRepository.saveAll(stats);

        // 3. 썸네일 일괄 저장
        List<ContentWithMetaData> itemsWithThumbnail = deduplicatedItems.stream()
                .filter(item -> StringUtils.hasText(item.thumbnailUrl()))
                .toList();
        if (!itemsWithThumbnail.isEmpty()) {
            Map<String, BinaryContent> savedThumbnailByUrl = binaryContentRepository.saveAll(
                    itemsWithThumbnail.stream()
                            .map(item -> BinaryContent.externalUrl(item.thumbnailUrl()))
                            .toList()
            ).stream().collect(Collectors.toMap(BinaryContent::getUrl, Function.identity()));
            itemsWithThumbnail.forEach(item ->
                    item.content().attachThumbnail(savedThumbnailByUrl.get(item.thumbnailUrl())));
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
