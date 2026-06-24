package com.codeit.team5.mopl.content.service;

import com.codeit.team5.mopl.binarycontent.BinaryContentStorage;
import com.codeit.team5.mopl.binarycontent.event.BinaryContentUploadEvent;
import com.codeit.team5.mopl.content.dto.request.ContentCreateRequest;
import com.codeit.team5.mopl.content.dto.response.ContentResponse;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentStats;
import com.codeit.team5.mopl.content.exception.EmptyTagException;
import com.codeit.team5.mopl.content.mapper.ContentMapper;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.content.repository.ContentStatsRepository;
import com.codeit.team5.mopl.tag.entity.Tag;
import com.codeit.team5.mopl.tag.repository.TagRepository;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ContentResponse create(ContentCreateRequest request, MultipartFile thumbnail) {
        Content content = contentRepository.save(Content.createByAdmin(
                request.type(),
                request.title(),
                request.description()
        ));

        if (thumbnail != null && !thumbnail.isEmpty()) {
            try {
                String key = binaryContentStorage.generateKey(content.getId(), thumbnail.getOriginalFilename());
                content.initThumbnail(binaryContentStorage.toUrl(key));
                eventPublisher.publishEvent(new BinaryContentUploadEvent(content.getId(), key, thumbnail.getBytes()));
            } catch (IOException e) {
                log.warn("썸네일 바이트 읽기 실패 - contentId: {}", content.getId(), e);
                content.failThumbnailUpload();
            }
        }

        List<String> tagNames = request.tags().stream()
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
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

        tagNames.forEach(name -> content.addTag(existingTags.get(name)));

        ContentStats stats = contentStatsRepository.save(ContentStats.create(content));

        return contentMapper.toDto(content, content.getContentTags(), stats);
    }

}
