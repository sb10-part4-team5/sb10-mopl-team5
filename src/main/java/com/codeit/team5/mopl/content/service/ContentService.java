package com.codeit.team5.mopl.content.service;

import com.codeit.team5.mopl.binarycontent.BinaryContentStorage;
import com.codeit.team5.mopl.binarycontent.StoragePrefix;
import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.binarycontent.event.BinaryContentUploadEvent;
import com.codeit.team5.mopl.binarycontent.repository.BinaryContentRepository;
import com.codeit.team5.mopl.content.dto.request.ContentCreateRequest;
import com.codeit.team5.mopl.content.dto.response.ContentResponse;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentStats;
import com.codeit.team5.mopl.content.exception.EmptyTagException;
import com.codeit.team5.mopl.content.mapper.ContentMapper;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.content.repository.ContentStatsRepository;
import com.codeit.team5.mopl.content.entity.ContentTag;
import com.codeit.team5.mopl.global.dto.FileResource;
import com.codeit.team5.mopl.tag.entity.Tag;
import com.codeit.team5.mopl.tag.repository.TagRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ContentService {

    private final ContentRepository contentRepository;
    private final ContentStatsRepository contentStatsRepository;
    private final TagRepository tagRepository;
    private final ContentMapper contentMapper;
    private final BinaryContentStorage binaryContentStorage;
    private final BinaryContentRepository binaryContentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ContentResponse create(ContentCreateRequest request, FileResource thumbnail) {
        Content content = contentRepository.save(Content.createByAdmin(
                request.type(),
                request.title(),
                request.description()
        ));

        if (thumbnail != null) {
            String key = binaryContentStorage.generateKey(StoragePrefix.THUMBNAIL, content.getId(), thumbnail.filename());
            BinaryContent binaryContent = binaryContentRepository.save(
                    BinaryContent.pending(binaryContentStorage.toUrl(key)));
            content.attachThumbnail(binaryContent);
            eventPublisher.publishEvent(new BinaryContentUploadEvent(binaryContent.getId(), key, thumbnail.bytes()));
        }

        attachTags(content, request.tags());

        ContentStats stats = contentStatsRepository.save(ContentStats.create());
        content.attachStats(stats);

        return contentMapper.toDto(content);
    }

    private void attachTags(Content content, List<String> rawTagNames) {
        List<String> tagNames = rawTagNames.stream()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(String::toLowerCase)
                .distinct()
                .toList();

        if (tagNames.isEmpty()) {
            throw new EmptyTagException();
        }

        Map<String, Tag> existingTags = tagRepository.findByNameIn(tagNames).stream()
                .collect(Collectors.toMap(Tag::getName, Function.identity()));

        List<Tag> newTags = tagNames.stream()
                .filter(name -> !existingTags.containsKey(name))
                .map(Tag::create)
                .toList();

        if (!newTags.isEmpty()) {
            tagRepository.saveAll(newTags).forEach(tag -> existingTags.put(tag.getName(), tag));
        }

        tagNames.forEach(name -> content.addTag(ContentTag.create(content, existingTags.get(name))));
    }
}
