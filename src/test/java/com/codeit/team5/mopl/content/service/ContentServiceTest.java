package com.codeit.team5.mopl.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.binarycontent.dto.UploadedBinaryContent;
import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.binarycontent.service.BinaryContentService;
import com.codeit.team5.mopl.binarycontent.entity.BinaryContentUploadStatus;
import com.codeit.team5.mopl.content.dto.request.ContentCreateRequest;
import com.codeit.team5.mopl.content.dto.request.ContentCursorRequest;
import com.codeit.team5.mopl.content.dto.request.ContentUpdateRequest;
import com.codeit.team5.mopl.content.dto.response.ContentResponse;
import com.codeit.team5.mopl.content.entity.ContentSortByType;
import com.codeit.team5.mopl.content.event.ContentDeletedEvent;
import com.codeit.team5.mopl.content.event.ContentUpsertedEvent;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentStats;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.content.exception.ContentNotFoundException;
import com.codeit.team5.mopl.content.exception.EmptyTagException;
import com.codeit.team5.mopl.content.mapper.ContentMapper;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.content.repository.ContentStatsRepository;
import com.codeit.team5.mopl.content.finder.ContentCacheFinder;
import com.codeit.team5.mopl.content.finder.ContentSearchFinder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ContentStatsRepository contentStatsRepository;

    @Mock
    private ContentTagService contentTagService;

    @Mock
    private ContentMapper contentMapper;

    @Mock
    private BinaryContentService binaryContentService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ContentCacheFinder contentCacheFinder;

    @Mock
    private ContentSearchFinder contentSearchFinder;

    @InjectMocks
    private ContentService contentService;

    // save()가 실제 저장처럼 UUID를 채워 반환하도록 한다.
    // ContentService.create()가 저장 직후 content.getId()로 이벤트를 발행하기 때문에
    // ID가 비어 있으면(NPE 방지 필요) 실제 영속화 결과를 흉내내야 한다.
    private Answer<Content> assignGeneratedId() {
        return invocation -> {
            Content content = invocation.getArgument(0);
            ReflectionTestUtils.setField(content, "id", UUID.randomUUID());
            return content;
        };
    }

    // --- CREATE ---
    @Test
    @DisplayName("콘텐츠 생성에 성공한다")
    void create_success() {
        // given
        ContentCreateRequest request = new ContentCreateRequest(
                ContentType.MOVIE, "테스트 영화", "테스트 설명", List.of("액션", "드라마")
        );
        UploadedBinaryContent thumbnail =
                new UploadedBinaryContent("thumbnails/test.jpg", "http://localhost:8080/thumbnails/test.jpg");
        ContentResponse expectedResponse = new ContentResponse(
                UUID.randomUUID(), ContentType.MOVIE, "테스트 영화", "테스트 설명",
                null, List.of("액션", "드라마"), 0.0, 0, 0
        );

        when(contentRepository.save(any(Content.class))).thenAnswer(assignGeneratedId());
        when(contentTagService.normalizeNames(List.of("액션", "드라마"))).thenReturn(List.of("액션", "드라마"));
        when(contentStatsRepository.save(any(ContentStats.class))).then(returnsFirstArg());
        when(binaryContentService.saveCompleted(thumbnail))
                .thenReturn(BinaryContent.completed("http://localhost:8080/thumbnails/test.jpg"));
        when(contentMapper.toDto(any(Content.class))).thenReturn(expectedResponse);

        // when
        ContentResponse result = contentService.create(request, thumbnail);

        // then
        assertThat(result).isSameAs(expectedResponse);

        ArgumentCaptor<Content> contentCaptor = ArgumentCaptor.forClass(Content.class);
        verify(contentRepository).save(contentCaptor.capture());
        Content savedContent = contentCaptor.getValue();
        assertThat(savedContent.getType()).isEqualTo(ContentType.MOVIE);
        assertThat(savedContent.getTitle()).isEqualTo("테스트 영화");
        assertThat(savedContent.getThumbnail()).isNotNull();

        verify(binaryContentService).saveCompleted(thumbnail);

        verify(contentTagService).attachTags(savedContent, List.of("액션", "드라마"));
        verify(contentStatsRepository).save(any(ContentStats.class));
        verify(eventPublisher).publishEvent(new ContentUpsertedEvent(List.of(savedContent.getId())));
    }

    @Test
    @DisplayName("thumbnail이 null이어도 콘텐츠 생성에 성공한다")
    void create_withoutThumbnail_success() {
        // given
        ContentCreateRequest request = new ContentCreateRequest(
                ContentType.TV_SERIES, "테스트 드라마", null, List.of("로맨스")
        );
        ContentResponse expectedResponse = new ContentResponse(
                UUID.randomUUID(), ContentType.TV_SERIES, "테스트 드라마",
                null, null, List.of("로맨스"), 0.0, 0, 0
        );

        when(contentRepository.save(any(Content.class))).thenAnswer(assignGeneratedId());
        when(contentTagService.normalizeNames(List.of("로맨스"))).thenReturn(List.of("로맨스"));
        when(contentStatsRepository.save(any(ContentStats.class))).then(returnsFirstArg());
        when(contentMapper.toDto(any(Content.class))).thenReturn(expectedResponse);

        // when
        ContentResponse result = contentService.create(request, null);

        // then
        assertThat(result).isSameAs(expectedResponse);
        verifyNoInteractions(binaryContentService);

        ArgumentCaptor<Content> contentCaptor = ArgumentCaptor.forClass(Content.class);
        verify(contentRepository).save(contentCaptor.capture());
        Content savedContent = contentCaptor.getValue();
        verify(eventPublisher).publishEvent(new ContentUpsertedEvent(List.of(savedContent.getId())));
    }

    @Test
    @DisplayName("정규화된 태그 이름으로 태그 연결을 위임한다")
    void create_delegatesNormalizedTagNamesToContentTagService() {
        // given
        ContentCreateRequest request = new ContentCreateRequest(
                ContentType.MOVIE, "테스트 영화", null, List.of("  Action  ", "DRAMA", "action")
        );
        when(contentRepository.save(any(Content.class))).thenAnswer(assignGeneratedId());
        when(contentTagService.normalizeNames(List.of("  Action  ", "DRAMA", "action")))
                .thenReturn(List.of("action", "drama"));
        when(contentStatsRepository.save(any(ContentStats.class))).then(returnsFirstArg());
        when(contentMapper.toDto(any(Content.class))).thenReturn(null);

        // when
        contentService.create(request, null);

        // then
        verify(contentTagService).attachTags(any(Content.class), eq(List.of("action", "drama")));
    }

    @Test
    @DisplayName("정규화 후 유효한 태그가 없으면 예외를 던진다")
    void create_allBlankTags_throwsException() {
        // given
        ContentCreateRequest request = new ContentCreateRequest(
                ContentType.MOVIE, "테스트 영화", null, List.of("   ", " ")
        );
        when(contentRepository.save(any(Content.class))).then(returnsFirstArg());
        when(contentTagService.normalizeNames(List.of("   ", " "))).thenReturn(List.of());

        // when & then
        assertThatThrownBy(() -> contentService.create(request, null))
                .isInstanceOf(EmptyTagException.class);

        verify(contentTagService, never()).attachTags(any(Content.class), anyList());
    }

    // --- UPDATE ---
    @Test
    @DisplayName("썸네일 없이 콘텐츠 수정에 성공한다")
    void update_withoutThumbnail_success() {
        // given
        UUID contentId = UUID.randomUUID();
        Content content = Content.createByAdmin(ContentType.MOVIE, "기존 제목", "기존 설명");
        ReflectionTestUtils.setField(content, "id", contentId);
        ContentUpdateRequest request = new ContentUpdateRequest("수정 제목", "수정 설명", List.of("SF"));
        ContentResponse expectedResponse = new ContentResponse(
                contentId, ContentType.MOVIE, "수정 제목", "수정 설명",
                null, List.of("sf"), 0.0, 0, 0
        );

        when(contentRepository.findWithStatsAndTagsById(contentId)).thenReturn(Optional.of(content));
        when(contentTagService.normalizeNames(List.of("SF"))).thenReturn(List.of("sf"));
        when(contentMapper.toDto(content)).thenReturn(expectedResponse);

        // when
        ContentResponse result = contentService.update(contentId, request, null);

        // then
        assertThat(result).isSameAs(expectedResponse);
        assertThat(content.getTitle()).isEqualTo("수정 제목");
        assertThat(content.getDescription()).isEqualTo("수정 설명");
        verifyNoInteractions(binaryContentService);
        verify(eventPublisher).publishEvent(new ContentUpsertedEvent(List.of(contentId)));
    }

    @Test
    @DisplayName("기존 썸네일이 없을 때 새 썸네일로 수정에 성공한다")
    void update_withNewThumbnail_noOldThumbnail_success() {
        // given
        UUID contentId = UUID.randomUUID();
        Content content = Content.createByAdmin(ContentType.MOVIE, "기존 제목", null);
        ReflectionTestUtils.setField(content, "id", contentId);
        UploadedBinaryContent thumbnail =
                new UploadedBinaryContent("thumbnails/new.jpg", "http://localhost/thumbnails/new.jpg");
        ContentResponse expectedResponse = new ContentResponse(
                contentId, ContentType.MOVIE, "수정 제목", null,
                "http://localhost/thumbnails/new.jpg",
                List.of("액션"), 0.0, 0, 0
        );

        when(contentRepository.findWithStatsAndTagsById(contentId)).thenReturn(Optional.of(content));
        when(contentTagService.normalizeNames(List.of("액션"))).thenReturn(List.of("액션"));
        when(binaryContentService.saveCompleted(thumbnail))
                .thenReturn(BinaryContent.completed("http://localhost/thumbnails/new.jpg"));
        when(contentMapper.toDto(content)).thenReturn(expectedResponse);

        // when
        ContentResponse result = contentService.update(contentId, new ContentUpdateRequest("수정 제목", null, List.of("액션")), thumbnail);

        // then
        assertThat(result).isSameAs(expectedResponse);
        assertThat(content.getThumbnail()).isNotNull();
        verify(binaryContentService).saveCompleted(thumbnail);
    }

    @Test
    @DisplayName("기존 썸네일이 있을 때 새 썸네일로 교체하면 기존 썸네일이 DELETED 상태가 된다")
    void update_withNewThumbnail_oldThumbnailMarkedDeleted() {
        // given
        UUID contentId = UUID.randomUUID();
        Content content = Content.createByAdmin(ContentType.MOVIE, "기존 제목", null);
        ReflectionTestUtils.setField(content, "id", contentId);
        BinaryContent oldThumbnail = BinaryContent.completed("http://localhost/thumbnails/old.jpg");
        content.attachThumbnail(oldThumbnail);
        UploadedBinaryContent thumbnail =
                new UploadedBinaryContent("thumbnails/new.jpg", "http://localhost/thumbnails/new.jpg");

        when(contentRepository.findWithStatsAndTagsById(contentId)).thenReturn(Optional.of(content));
        when(contentTagService.normalizeNames(List.of("액션"))).thenReturn(List.of("액션"));
        when(binaryContentService.saveCompleted(thumbnail))
                .thenReturn(BinaryContent.completed("http://localhost/thumbnails/new.jpg"));
        when(contentMapper.toDto(content)).thenReturn(null);

        // when
        contentService.update(contentId, new ContentUpdateRequest("수정 제목", null, List.of("액션")), thumbnail);

        // then
        assertThat(oldThumbnail.getUploadStatus()).isEqualTo(BinaryContentUploadStatus.DELETED);
        assertThat(content.getThumbnail()).isNotSameAs(oldThumbnail);
    }

    @Test
    @DisplayName("정규화된 태그 이름으로 태그 갱신을 위임한다")
    void update_delegatesNormalizedTagNamesToContentTagService() {
        // given
        UUID contentId = UUID.randomUUID();
        Content content = Content.createByAdmin(ContentType.MOVIE, "기존 제목", null);
        ReflectionTestUtils.setField(content, "id", contentId);
        ContentUpdateRequest request = new ContentUpdateRequest("기존 제목", null, List.of("  Action  ", "action"));

        when(contentRepository.findWithStatsAndTagsById(contentId)).thenReturn(Optional.of(content));
        when(contentTagService.normalizeNames(List.of("  Action  ", "action"))).thenReturn(List.of("action"));
        when(contentMapper.toDto(content)).thenReturn(null);

        // when
        contentService.update(contentId, request, null);

        // then
        verify(contentTagService).updateTags(content, List.of("action"));
    }

    @Test
    @DisplayName("존재하지 않는 콘텐츠 수정 시 예외를 던진다")
    void update_notFound_throwsException() {
        // given
        UUID contentId = UUID.randomUUID();
        when(contentRepository.findWithStatsAndTagsById(contentId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> contentService.update(
                contentId, new ContentUpdateRequest("제목", null, List.of("액션")), null))
                .isInstanceOf(ContentNotFoundException.class);

        verifyNoInteractions(contentTagService, binaryContentService, eventPublisher, contentMapper);
    }

    @Test
    @DisplayName("수정 시 정규화 후 유효한 태그가 없으면 예외를 던지고 엔티티는 변경되지 않는다")
    void update_allBlankTags_throwsException() {
        // given
        UUID contentId = UUID.randomUUID();
        Content content = Content.createByAdmin(ContentType.MOVIE, "기존 제목", null);
        when(contentRepository.findWithStatsAndTagsById(contentId)).thenReturn(Optional.of(content));
        when(contentTagService.normalizeNames(List.of("  ", " "))).thenReturn(List.of());

        // when & then
        assertThatThrownBy(() -> contentService.update(
                contentId, new ContentUpdateRequest("수정 제목", null, List.of("  ", " ")), null))
                .isInstanceOf(EmptyTagException.class);

        assertThat(content.getTitle()).isEqualTo("기존 제목");
        verify(contentTagService, never()).updateTags(any(Content.class), anyList());
        verifyNoInteractions(binaryContentService, eventPublisher, contentMapper);
    }

    // --- FIND BY ID ---
    @Test
    @DisplayName("존재하는 콘텐츠 ID로 단건 조회에 성공한다")
    void findById_success() {
        // given
        UUID contentId = UUID.randomUUID();
        Content content = Content.createByAdmin(ContentType.MOVIE, "테스트 영화", "설명");
        ContentResponse expectedResponse = new ContentResponse(
                contentId, ContentType.MOVIE, "테스트 영화", "설명",
                null, List.of("액션"), 4.5, 10, 100L
        );

        when(contentRepository.findWithStatsAndTagsById(contentId)).thenReturn(Optional.of(content));
        when(contentMapper.toDto(content)).thenReturn(expectedResponse);

        // when
        ContentResponse result = contentService.findById(contentId);

        // then
        assertThat(result).isSameAs(expectedResponse);
        verify(contentRepository).findWithStatsAndTagsById(contentId);
        verify(contentMapper).toDto(content);
    }

    @Test
    @DisplayName("존재하지 않는 콘텐츠 ID로 단건 조회 시 예외를 던진다")
    void findById_notFound_throwsException() {
        // given
        UUID contentId = UUID.randomUUID();
        when(contentRepository.findWithStatsAndTagsById(contentId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> contentService.findById(contentId))
                .isInstanceOf(ContentNotFoundException.class);

        verifyNoInteractions(contentMapper);
    }

    // --- FIND CONTENTS ---
    @Test
    @DisplayName("다음 페이지가 없는 경우 hasNext=false로 커서 응답을 반환한다")
    void findContents_noNextPage_success() {
        // given: limit이 첫 페이지 캐시 기본값(20)이 아니므로 캐시를 타지 않고 DB를 직접 조회한다
        ContentCursorRequest request = new ContentCursorRequest(
                null, null, null, null, null,
                10, Sort.Direction.DESC, ContentSortByType.CREATED_AT
        );
        List<Content> contents = List.of(
                Content.createByAdmin(ContentType.MOVIE, "영화1", null),
                Content.createByAdmin(ContentType.MOVIE, "영화2", null)
        );
        CursorResponse<ContentResponse> expectedResponse = new CursorResponse<>(
                List.of(), null, null, false, 2L, "createdAt", "DESCENDING"
        );

        when(contentRepository.findContents(request, 11)).thenReturn(contents);
        when(contentRepository.countContents(request)).thenReturn(2L);
        when(contentMapper.toCursor(contents, false, 2L, ContentSortByType.CREATED_AT, Sort.Direction.DESC))
                .thenReturn(expectedResponse);

        // when
        CursorResponse<ContentResponse> result = contentService.findContents(request);

        // then
        assertThat(result).isSameAs(expectedResponse);
        verify(contentRepository).findContents(request, 11);
        verify(contentRepository).countContents(request);
        verify(contentMapper).toCursor(contents, false, 2L, ContentSortByType.CREATED_AT, Sort.Direction.DESC);
        verifyNoInteractions(contentCacheFinder, contentSearchFinder);
    }

    @Test
    @DisplayName("키워드가 있으면 검색 파인더에 위임한다")
    void findContents_keywordSearch_delegatesToSearchFinder() {
        // given
        ContentCursorRequest request = new ContentCursorRequest(
                null, "영화", null, null, null,
                20, Sort.Direction.DESC, ContentSortByType.WATCHER_COUNT
        );
        CursorResponse<ContentResponse> expectedResponse = new CursorResponse<>(
                List.of(), null, null, false, 0L, "watcherCount", "DESCENDING"
        );

        when(contentSearchFinder.search(request)).thenReturn(expectedResponse);

        // when
        CursorResponse<ContentResponse> result = contentService.findContents(request);

        // then
        assertThat(result).isSameAs(expectedResponse);
        verify(contentSearchFinder).search(request);
        verifyNoInteractions(contentRepository, contentMapper, contentCacheFinder);
    }

    @Test
    @DisplayName("필터·커서 없이 기본 limit으로 첫 페이지를 조회하면 캐시 스토어에 위임한다")
    void findContents_cacheableFirstPage_delegatesToCacheStore() {
        // given
        ContentCursorRequest request = new ContentCursorRequest(
                null, null, null, null, null,
                ContentCacheFinder.FIRST_PAGE_LIMIT, Sort.Direction.DESC, ContentSortByType.WATCHER_COUNT
        );
        CursorResponse<ContentResponse> expectedResponse = new CursorResponse<>(
                List.of(), null, null, false, 0L, "watcherCount", "DESCENDING"
        );

        when(contentCacheFinder.getFirstPage(ContentSortByType.WATCHER_COUNT, Sort.Direction.DESC))
                .thenReturn(expectedResponse);

        // when
        CursorResponse<ContentResponse> result = contentService.findContents(request);

        // then
        assertThat(result).isSameAs(expectedResponse);
        verify(contentCacheFinder).getFirstPage(ContentSortByType.WATCHER_COUNT, Sort.Direction.DESC);
        verifyNoInteractions(contentRepository, contentMapper, contentSearchFinder);
    }

    @Test
    @DisplayName("다음 페이지가 있는 경우 hasNext=true이고 limit개만 반환한다")
    void findContents_hasNextPage_success() {
        // given
        int limit = 2;
        ContentCursorRequest request = new ContentCursorRequest(
                null, null, null, null, null,
                limit, Sort.Direction.DESC, ContentSortByType.CREATED_AT
        );
        // limit+1 = 3개 반환 → hasNext=true
        List<Content> fetched = new ArrayList<>();
        for (int i = 0; i < limit + 1; i++) {
            fetched.add(Content.createByAdmin(ContentType.MOVIE, "영화" + i, null));
        }
        List<Content> page = fetched.subList(0, limit);
        CursorResponse<ContentResponse> expectedResponse = new CursorResponse<>(
                List.of(), "cursor", "idAfter", true, 5L, "createdAt", "DESCENDING"
        );

        when(contentRepository.findContents(request, limit + 1)).thenReturn(fetched);
        when(contentRepository.countContents(request)).thenReturn(5L);
        when(contentMapper.toCursor(page, true, 5L, ContentSortByType.CREATED_AT, Sort.Direction.DESC))
                .thenReturn(expectedResponse);

        // when
        CursorResponse<ContentResponse> result = contentService.findContents(request);

        // then
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo("cursor");
        verify(contentMapper).toCursor(page, true, 5L, ContentSortByType.CREATED_AT, Sort.Direction.DESC);
    }

    @Test
    @DisplayName("조회 결과가 없으면 빈 목록과 totalCount=0을 반환한다")
    void findContents_emptyResult() {
        // given: 키워드 없이 타입 필터만 있어 캐시 첫 페이지 조건(isCacheableFirstPage)에서 제외되고 DB로 직접 조회한다
        ContentCursorRequest request = new ContentCursorRequest(
                ContentType.MOVIE, null, null, null, null,
                20, Sort.Direction.DESC, ContentSortByType.CREATED_AT
        );
        CursorResponse<ContentResponse> expectedResponse = new CursorResponse<>(
                List.of(), null, null, false, 0L, "createdAt", "DESCENDING"
        );

        when(contentRepository.findContents(request, 21)).thenReturn(List.of());
        when(contentRepository.countContents(request)).thenReturn(0L);
        when(contentMapper.toCursor(List.of(), false, 0L, ContentSortByType.CREATED_AT, Sort.Direction.DESC))
                .thenReturn(expectedResponse);

        // when
        CursorResponse<ContentResponse> result = contentService.findContents(request);

        // then
        assertThat(result.data()).isEmpty();
        assertThat(result.totalCount()).isZero();
        assertThat(result.hasNext()).isFalse();
    }

    // --- DELETE ---
    @Test
    @DisplayName("썸네일 없는 콘텐츠 삭제에 성공한다")
    void delete_withoutThumbnail_success() {
        // given
        UUID contentId = UUID.randomUUID();
        Content content = Content.createByAdmin(ContentType.MOVIE, "테스트 영화", null);
        ReflectionTestUtils.setField(content, "id", contentId);
        when(contentRepository.findWithStatsAndTagsById(contentId)).thenReturn(Optional.of(content));

        // when
        contentService.delete(contentId);

        // then
        verify(contentRepository).delete(content);
        verifyNoInteractions(binaryContentService);
        verify(eventPublisher).publishEvent(new ContentDeletedEvent(contentId));
    }

    @Test
    @DisplayName("썸네일 있는 콘텐츠 삭제 시 썸네일이 DELETED 상태가 된다")
    void delete_withThumbnail_marksDeletedAndDeletes() {
        // given
        UUID contentId = UUID.randomUUID();
        Content content = Content.createByAdmin(ContentType.MOVIE, "테스트 영화", null);
        BinaryContent thumbnail = BinaryContent.completed("http://localhost/thumbnails/thumb.jpg");
        content.attachThumbnail(thumbnail);
        when(contentRepository.findWithStatsAndTagsById(contentId)).thenReturn(Optional.of(content));

        // when
        contentService.delete(contentId);

        // then
        assertThat(thumbnail.getUploadStatus()).isEqualTo(BinaryContentUploadStatus.DELETED);
        verify(contentRepository).delete(content);
        verify(eventPublisher).publishEvent(any(com.codeit.team5.mopl.content.event.ContentDeletedEvent.class));
        verifyNoInteractions(binaryContentService);
    }

    @Test
    @DisplayName("존재하지 않는 콘텐츠 삭제 시 예외를 던진다")
    void delete_notFound_throwsException() {
        // given
        UUID contentId = UUID.randomUUID();
        when(contentRepository.findWithStatsAndTagsById(contentId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> contentService.delete(contentId))
                .isInstanceOf(ContentNotFoundException.class);

        verify(contentRepository, never()).delete(any());
    }
}
