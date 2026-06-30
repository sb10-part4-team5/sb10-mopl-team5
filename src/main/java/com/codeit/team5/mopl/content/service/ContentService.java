package com.codeit.team5.mopl.content.service;

import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.binarycontent.entity.BinaryContentUploadStatus;
import com.codeit.team5.mopl.binarycontent.event.BinaryContentUploadEvent;
import com.codeit.team5.mopl.binarycontent.repository.BinaryContentRepository;
import com.codeit.team5.mopl.binarycontent.storage.BinaryContentStorage;
import com.codeit.team5.mopl.binarycontent.storage.GeneratedKey;
import com.codeit.team5.mopl.binarycontent.storage.StorageDirectory;
import com.codeit.team5.mopl.binarycontent.storage.StorageKeyFactory;
import com.codeit.team5.mopl.content.dto.request.ContentCreateRequest;
import com.codeit.team5.mopl.content.dto.request.ContentCursorRequest;
import com.codeit.team5.mopl.content.dto.request.ContentUpdateRequest;
import com.codeit.team5.mopl.content.dto.response.ContentResponse;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentStats;
import com.codeit.team5.mopl.content.entity.ContentTag;
import com.codeit.team5.mopl.content.exception.ContentNotFoundException;
import com.codeit.team5.mopl.content.exception.EmptyTagException;
import com.codeit.team5.mopl.content.exception.TooManyTagsException;
import com.codeit.team5.mopl.content.mapper.ContentMapper;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.content.repository.ContentStatsRepository;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.global.dto.FileRequest;
import com.codeit.team5.mopl.tag.entity.Tag;
import com.codeit.team5.mopl.tag.repository.TagRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자가 직접 등록하는 콘텐츠의 CRUD를 담당하는 서비스.
 *
 * <p>외부 API 수집 콘텐츠({@link TmdbContentService}, {@link SportsDbContentService})와 달리
 * 썸네일 이미지는 S3에 비동기 업로드(이벤트 기반)되며, 태그는 최대 10개 제한이 적용된다.</p>
 */
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

    private static final String SECONDARY_SORT_FIELD = "id";

    /**
     * 관리자가 콘텐츠를 직접 생성한다.
     *
     * @param request 제목·설명·타입·태그 정보
     * @param image   썸네일 이미지 (null 허용)
     * @return 생성된 콘텐츠 응답 DTO
     */
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

        ContentStats stats = contentStatsRepository.save(ContentStats.create(content));
        content.attachStats(stats);

        return contentMapper.toDto(content);
    }

    /**
     * 콘텐츠 정보를 수정한다. 이미지가 제공되면 기존 썸네일을 DELETED 상태로 변경 후 교체한다.
     *
     * @param contentId 수정할 콘텐츠 UUID
     * @param request   변경할 제목·설명·태그 정보
     * @param image     새 썸네일 이미지 (null이면 기존 이미지 유지)
     * @return 수정된 콘텐츠 응답 DTO
     * @throws ContentNotFoundException 콘텐츠가 존재하지 않을 때
     */
    @Transactional
    public ContentResponse update(UUID contentId, ContentUpdateRequest request, FileRequest image) {
        Content content = contentRepository.findWithStatsAndTagsById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));

        List<String> tagNames = normalizeTagNames(request.tags());

        content.update(request.title(), request.description());
        updateTags(content, tagNames);

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

    /**
     * 콘텐츠 단건을 조회한다.
     *
     * @param contentId 조회할 콘텐츠 UUID
     * @return 콘텐츠 응답 DTO (stats, tags 포함)
     * @throws ContentNotFoundException 콘텐츠가 존재하지 않을 때
     */
    public ContentResponse findById(UUID contentId) {
        Content content = contentRepository.findWithStatsAndTagsById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
        return contentMapper.toDto(content);
    }

    /**
     * 커서 기반 페이지네이션으로 콘텐츠 목록을 조회한다.
     *
     * @param request 커서·정렬·필터·limit 조건
     * @return 커서 응답 (콘텐츠 목록, hasNext, totalCount 포함)
     */
    public CursorResponse<ContentResponse> findContents(ContentCursorRequest request) {
        int fetchLimit = request.limit() + 1;
        List<Content> fetched = contentRepository.findContents(request, fetchLimit);
        boolean hasNext = fetched.size() > request.limit();
        List<Content> page = hasNext ? fetched.subList(0, request.limit()) : fetched;
        long totalCount = contentRepository.countContents(request);
        return contentMapper.toCursor(page, hasNext, totalCount, request.sortBy(), request.sortDirection());
    }

    /**
     * 콘텐츠를 삭제한다. 연결된 썸네일이 있으면 DELETED 상태로 마킹 후 삭제한다.
     *
     * @param contentId 삭제할 콘텐츠 UUID
     * @throws ContentNotFoundException 콘텐츠가 존재하지 않을 때
     */
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
        BinaryContent thumbnailImage = binaryContentRepository.save(
                BinaryContent.pending(binaryContentStorage.toUrl(generated.key())));

        eventPublisher.publishEvent(
                new BinaryContentUploadEvent(thumbnailImage.getId(), generated.key(), image.bytes(), generated.contentType()));
        // TODO(업로드 실패 대응): 비동기 업로드 실패 시 보상 트랜잭션으로 썸네일 이미지 롤백

        return thumbnailImage;
    }

    private void attachTags(Content content, List<String> rawTagNames) {
        List<String> tagNames = normalizeTagNames(rawTagNames);
        insertTags(content, tagNames);
    }

    private void updateTags(Content content, List<String> requestedNames) {
        Set<String> currentNames = content.getContentTags().stream()
                .map(ct -> ct.getTag().getName())
                .collect(Collectors.toSet());

        Set<String> requestedSet = new HashSet<>(requestedNames);
        content.getContentTags().removeIf(ct -> !requestedSet.contains(ct.getTag().getName()));

        List<String> toAdd = requestedNames.stream()
                .filter(name -> !currentNames.contains(name))
                .toList();

        if (!toAdd.isEmpty()) {
            insertTags(content, toAdd);
        }
    }

    private void insertTags(Content content, List<String> tagNames) {
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

    private List<String> normalizeTagNames(List<String> rawTagNames) {
        List<String> tagNames = rawTagNames.stream()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(String::toLowerCase)
                .distinct()
                .toList();

        if (tagNames.isEmpty()) throw new EmptyTagException();
        if (tagNames.size() > 10) throw new TooManyTagsException();
        return tagNames;
    }

}
