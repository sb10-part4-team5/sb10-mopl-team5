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
import com.codeit.team5.mopl.content.exception.TooManyTagsException;
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
    public ContentResponse create(ContentCreateRequest request, FileRequest image) {
        Content content = contentRepository.save(Content.createByAdmin(
                request.type(),
                request.title(),
                request.description()
        ));

        if (image != null) {
            content.attachThumbnail(storeThumbnailImage(content.getId(), image));
        }

        attachTags(content, request.tags());

        ContentStats stats = contentStatsRepository.save(ContentStats.create());
        content.attachStats(stats);

        return contentMapper.toDto(content);
    }

    @Transactional
    public ContentResponse update(UUID contentId, ContentUpdateRequest request, FileRequest image) {
        Content content = contentRepository.findWithStatsAndTagsById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));

        content.update(request.title(), request.description());
        content.clearTags();
        attachTags(content, request.tags());

        if (image != null) {
            // TODO(고아 정리): 비정상적인 상태를 가진 BinaryContent를 배치로 정리 (DB/S3 누적 방지)
            BinaryContent oldThumbnail = content.getThumbnail();
            if (oldThumbnail != null) {
                oldThumbnail.updateUploadStatus(BinaryContentUploadStatus.DELETED);
            }
            content.attachThumbnail(storeThumbnailImage(content.getId(), image));
        }

        return contentMapper.toDto(content);
    }

    @Transactional
    public void delete(UUID contentId) {
        Content content = contentRepository.findWithStatsAndTagsById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
        BinaryContent oldThumbnail = content.getThumbnail();
        if (oldThumbnail != null) {
            oldThumbnail.updateUploadStatus(BinaryContentUploadStatus.DELETED);
        }
        contentRepository.delete(content);
    }

    private BinaryContent storeThumbnailImage(UUID contentId, FileRequest image) {
        GeneratedKey generated = storageKeyFactory.generate(StorageDirectory.THUMBNAIL, contentId, image.filename());
        BinaryContent profileImage = binaryContentRepository.save(
                BinaryContent.pending(binaryContentStorage.toUrl(generated.key())));

        eventPublisher.publishEvent(
                new BinaryContentUploadEvent(profileImage.getId(), generated.key(), image.bytes(), generated.contentType()));
        // TODO(업로드 실패 대응): 비동기 업로드 실패 시 보상 트랜잭션으로 썸네일 이미지 롤백

        return profileImage;
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
        if (tagNames.size() > 10) {
            throw new TooManyTagsException();
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
