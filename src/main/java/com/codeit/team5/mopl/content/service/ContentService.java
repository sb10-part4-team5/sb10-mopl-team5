package com.codeit.team5.mopl.content.service;

import com.codeit.team5.mopl.binarycontent.storage.BinaryContentStorage;
import com.codeit.team5.mopl.binarycontent.storage.GeneratedKey;
import com.codeit.team5.mopl.binarycontent.storage.StorageDirectory;
import com.codeit.team5.mopl.binarycontent.storage.StorageKeyFactory;
import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.binarycontent.entity.BinaryContentUploadStatus;
import com.codeit.team5.mopl.binarycontent.event.BinaryContentUploadEvent;
import com.codeit.team5.mopl.binarycontent.repository.BinaryContentRepository;
import com.codeit.team5.mopl.content.dto.request.ContentCreateRequest;
import com.codeit.team5.mopl.content.dto.request.ContentUpdateRequest;
import com.codeit.team5.mopl.content.dto.response.ContentResponse;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentStats;
import com.codeit.team5.mopl.content.exception.ContentNotFoundException;
import com.codeit.team5.mopl.content.exception.EmptyTagException;
import com.codeit.team5.mopl.content.mapper.ContentMapper;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.content.repository.ContentStatsRepository;
import com.codeit.team5.mopl.content.entity.ContentTag;
import com.codeit.team5.mopl.global.dto.FileRequest;
import com.codeit.team5.mopl.tag.entity.Tag;
import com.codeit.team5.mopl.tag.repository.TagRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private final StorageKeyFactory storageKeyFactory;
    private final BinaryContentRepository binaryContentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ContentResponse create(ContentCreateRequest request, FileRequest thumbnail) {
        Content content = contentRepository.save(Content.createByAdmin(
                request.type(),
                request.title(),
                request.description()
        ));

        if (thumbnail != null) {
            GeneratedKey generated = storageKeyFactory.generate(StorageDirectory.THUMBNAIL, content.getId(), thumbnail.filename());
            BinaryContent binaryContent = binaryContentRepository.save(
                    BinaryContent.pending(binaryContentStorage.toUrl(generated.key())));
            content.attachThumbnail(binaryContent);
            eventPublisher.publishEvent(new BinaryContentUploadEvent(
                    binaryContent.getId(), generated.key(), thumbnail.bytes(), generated.contentType()));
        }

        attachTags(content, request.tags());

        ContentStats stats = contentStatsRepository.save(ContentStats.create());
        content.attachStats(stats);

        return contentMapper.toDto(content);
    }

    @Transactional
    public ContentResponse update(UUID contentId, ContentUpdateRequest request, MultipartFile thumbnail) {
        Content content = contentRepository.findWithStatsAndTagsById(contentId)
                .orElseThrow(ContentNotFoundException::new);

        content.update(request.title(), request.description());
        content.clearTags();
        attachTags(content, request.tags());

        if (thumbnail != null && !thumbnail.isEmpty()) {
            try {
                BinaryContent oldThumbnail = content.getThumbnail();
                if (oldThumbnail != null) {
                    oldThumbnail.updateUploadStatus(BinaryContentUploadStatus.DELETED);
                }
                String key = binaryContentStorage.generateKey(contentId, thumbnail.getOriginalFilename());
                BinaryContent binaryContent = binaryContentRepository.save(
                        BinaryContent.pending(binaryContentStorage.toUrl(key)));
                content.attachThumbnail(binaryContent);
                eventPublisher.publishEvent(new BinaryContentUploadEvent(binaryContent.getId(), key, thumbnail.getBytes()));
            } catch (IOException e) {
                log.warn("썸네일 바이트 읽기 실패 - contentId: {}", contentId, e);
            }
        }

        return contentMapper.toDto(content);
    }

    @Transactional
    public void delete(UUID contentId) {
        Content content = contentRepository.findWithStatsAndTagsById(contentId)
                .orElseThrow(ContentNotFoundException::new);
        BinaryContent oldThumbnail = content.getThumbnail();
        if (oldThumbnail != null) {
            oldThumbnail.updateUploadStatus(BinaryContentUploadStatus.DELETED);
        }
        contentRepository.delete(content);
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
