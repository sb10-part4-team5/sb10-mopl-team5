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
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentStats;
import com.codeit.team5.mopl.content.entity.ContentTag;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.content.exception.ContentNotFoundException;
import com.codeit.team5.mopl.content.exception.EmptyTagException;
import com.codeit.team5.mopl.content.mapper.ContentMapper;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.content.repository.ContentStatsRepository;
import com.codeit.team5.mopl.tag.entity.Tag;
import com.codeit.team5.mopl.tag.repository.TagRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ContentStatsRepository contentStatsRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private ContentMapper contentMapper;

    @Mock
    private BinaryContentService binaryContentService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ContentService contentService;

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
        Tag actionTag = Tag.create("액션");
        Tag dramaTag = Tag.create("드라마");
        ContentResponse expectedResponse = new ContentResponse(
                UUID.randomUUID(), ContentType.MOVIE, "테스트 영화", "테스트 설명",
                null, List.of("액션", "드라마"), 0.0, 0, 0
        );

        when(contentRepository.save(any(Content.class))).then(returnsFirstArg());
        when(tagRepository.findByNameIn(List.of("액션", "드라마"))).thenReturn(List.of(actionTag));
        when(tagRepository.saveAll(anyList())).thenReturn(List.of(dramaTag));
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

        verify(tagRepository).findByNameIn(List.of("액션", "드라마"));
        verify(contentStatsRepository).save(any(ContentStats.class));
    }

    @Test
    @DisplayName("thumbnail이 null이어도 콘텐츠 생성에 성공한다")
    void create_withoutThumbnail_success() {
        // given
        ContentCreateRequest request = new ContentCreateRequest(
                ContentType.TV_SERIES, "테스트 드라마", null, List.of("로맨스")
        );
        Tag romanceTag = Tag.create("로맨스");
        ContentResponse expectedResponse = new ContentResponse(
                UUID.randomUUID(), ContentType.TV_SERIES, "테스트 드라마",
                null, null, List.of("로맨스"), 0.0, 0, 0
        );

        when(contentRepository.save(any(Content.class))).then(returnsFirstArg());
        when(tagRepository.findByNameIn(List.of("로맨스"))).thenReturn(List.of(romanceTag));
        when(contentStatsRepository.save(any(ContentStats.class))).then(returnsFirstArg());
        when(contentMapper.toDto(any(Content.class))).thenReturn(expectedResponse);

        // when
        ContentResponse result = contentService.create(request, null);

        // then
        assertThat(result).isSameAs(expectedResponse);
        verifyNoInteractions(binaryContentService, eventPublisher);
    }

    @Test
    @DisplayName("이미 존재하는 태그는 새로 저장하지 않는다")
    void create_existingTag_doesNotSaveNewTag() {
        // given
        ContentCreateRequest request = new ContentCreateRequest(
                ContentType.MOVIE, "테스트 영화", null, List.of("액션")
        );
        when(contentRepository.save(any(Content.class))).then(returnsFirstArg());
        when(tagRepository.findByNameIn(List.of("액션"))).thenReturn(List.of(Tag.create("액션")));
        when(contentStatsRepository.save(any(ContentStats.class))).then(returnsFirstArg());
        when(contentMapper.toDto(any(Content.class))).thenReturn(null);

        // when
        contentService.create(request, null);

        // then
        verify(tagRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("존재하지 않는 태그는 새로 저장한다")
    void create_newTag_savesTag() {
        // given
        ContentCreateRequest request = new ContentCreateRequest(
                ContentType.MOVIE, "테스트 영화", null, List.of("새태그1", "새태그2")
        );
        when(contentRepository.save(any(Content.class))).then(returnsFirstArg());
        when(tagRepository.findByNameIn(anyList())).thenReturn(List.of());
        when(tagRepository.saveAll(anyList())).then(returnsFirstArg());
        when(contentStatsRepository.save(any(ContentStats.class))).then(returnsFirstArg());
        when(contentMapper.toDto(any(Content.class))).thenReturn(null);

        // when
        contentService.create(request, null);

        // then
        ArgumentCaptor<List<Tag>> captor = ArgumentCaptor.forClass(List.class);
        verify(tagRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    @DisplayName("중복된 태그가 요청에 포함되어도 한 번만 저장한다")
    void create_duplicateTags_savesOnce() {
        // given
        ContentCreateRequest request = new ContentCreateRequest(
                ContentType.MOVIE, "테스트 영화", null, List.of("액션", "액션")
        );
        when(contentRepository.save(any(Content.class))).then(returnsFirstArg());
        when(tagRepository.findByNameIn(List.of("액션"))).thenReturn(List.of());
        when(tagRepository.saveAll(anyList())).then(returnsFirstArg());
        when(contentStatsRepository.save(any(ContentStats.class))).then(returnsFirstArg());
        when(contentMapper.toDto(any(Content.class))).thenReturn(null);

        // when
        contentService.create(request, null);

        // then
        ArgumentCaptor<List<Tag>> captor = ArgumentCaptor.forClass(List.class);
        verify(tagRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
    }

    @Test
    @DisplayName("정규화 후 유효한 태그가 없으면 예외를 던진다")
    void create_allBlankTags_throwsException() {
        // given
        ContentCreateRequest request = new ContentCreateRequest(
                ContentType.MOVIE, "테스트 영화", null, List.of("   ", " ")
        );
        when(contentRepository.save(any(Content.class))).then(returnsFirstArg());

        // when & then
        assertThatThrownBy(() -> contentService.create(request, null))
                .isInstanceOf(EmptyTagException.class);

        verify(tagRepository, never()).findByNameIn(anyList());
        verify(tagRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("태그는 앞뒤 공백 제거 및 소문자로 정규화되어 저장된다")
    void create_tagsNormalized() {
        // given
        ContentCreateRequest request = new ContentCreateRequest(
                ContentType.MOVIE, "테스트 영화", null, List.of("  Action  ", "DRAMA")
        );
        when(contentRepository.save(any(Content.class))).then(returnsFirstArg());
        when(tagRepository.findByNameIn(List.of("action", "drama"))).thenReturn(List.of());
        when(tagRepository.saveAll(anyList())).then(returnsFirstArg());
        when(contentStatsRepository.save(any(ContentStats.class))).then(returnsFirstArg());
        when(contentMapper.toDto(any(Content.class))).thenReturn(null);

        // when
        contentService.create(request, null);

        // then
        verify(tagRepository).findByNameIn(List.of("action", "drama"));
    }

    // --- UPDATE ---
    @Test
    @DisplayName("썸네일 없이 콘텐츠 수정에 성공한다")
    void update_withoutThumbnail_success() {
        // given
        UUID contentId = UUID.randomUUID();
        Content content = Content.createByAdmin(ContentType.MOVIE, "기존 제목", "기존 설명");
        ContentUpdateRequest request = new ContentUpdateRequest("수정 제목", "수정 설명", List.of("SF"));
        ContentResponse expectedResponse = new ContentResponse(
                contentId, ContentType.MOVIE, "수정 제목", "수정 설명",
                null, List.of("sf"), 0.0, 0, 0
        );

        when(contentRepository.findWithStatsAndTagsById(contentId)).thenReturn(Optional.of(content));
        when(tagRepository.findByNameIn(List.of("sf"))).thenReturn(List.of(Tag.create("sf")));
        when(contentMapper.toDto(content)).thenReturn(expectedResponse);

        // when
        ContentResponse result = contentService.update(contentId, request, null);

        // then
        assertThat(result).isSameAs(expectedResponse);
        assertThat(content.getTitle()).isEqualTo("수정 제목");
        assertThat(content.getDescription()).isEqualTo("수정 설명");
        verifyNoInteractions(binaryContentService, eventPublisher);
    }

    @Test
    @DisplayName("기존 썸네일이 없을 때 새 썸네일로 수정에 성공한다")
    void update_withNewThumbnail_noOldThumbnail_success() {
        // given
        UUID contentId = UUID.randomUUID();
        Content content = Content.createByAdmin(ContentType.MOVIE, "기존 제목", null);
        UploadedBinaryContent thumbnail =
                new UploadedBinaryContent("thumbnails/new.jpg", "http://localhost/thumbnails/new.jpg");
        ContentResponse expectedResponse = new ContentResponse(
                contentId, ContentType.MOVIE, "수정 제목", null,
                "http://localhost/thumbnails/new.jpg",
                List.of("액션"), 0.0, 0, 0
        );

        when(contentRepository.findWithStatsAndTagsById(contentId)).thenReturn(Optional.of(content));
        when(tagRepository.findByNameIn(List.of("액션"))).thenReturn(List.of(Tag.create("액션")));
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
        BinaryContent oldThumbnail = BinaryContent.completed("http://localhost/thumbnails/old.jpg");
        content.attachThumbnail(oldThumbnail);
        UploadedBinaryContent thumbnail =
                new UploadedBinaryContent("thumbnails/new.jpg", "http://localhost/thumbnails/new.jpg");

        when(contentRepository.findWithStatsAndTagsById(contentId)).thenReturn(Optional.of(content));
        when(tagRepository.findByNameIn(List.of("액션"))).thenReturn(List.of(Tag.create("액션")));
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
    @DisplayName("수정 시 유지되는 태그는 delete/insert 없이 그대로 유지된다")
    void update_retainedTags_notReinserted() {
        // given
        UUID contentId = UUID.randomUUID();
        Content content = Content.createByAdmin(ContentType.MOVIE, "기존 제목", null);
        ReflectionTestUtils.setField(content, "id", contentId);

        Tag actionTag = tagWithId("액션");
        Tag sfTag = tagWithId("sf");
        content.addTag(ContentTag.create(content, actionTag));  // 유지될 태그
        content.addTag(ContentTag.create(content, sfTag));      // 제거될 태그

        // 요청: 액션 유지 + 코미디 추가 (sf 제거)
        ContentUpdateRequest request = new ContentUpdateRequest("기존 제목", null, List.of("액션", "코미디"));
        Tag comedyTag = tagWithId("코미디");

        when(contentRepository.findWithStatsAndTagsById(contentId)).thenReturn(Optional.of(content));
        when(tagRepository.findByNameIn(List.of("코미디"))).thenReturn(List.of(comedyTag));
        when(contentMapper.toDto(content)).thenReturn(null);

        // when
        contentService.update(contentId, request, null);

        // then: 추가할 태그만 조회
        verify(tagRepository).findByNameIn(List.of("코미디"));
        verify(tagRepository, never()).saveAll(anyList());

        Set<String> finalTagNames = content.getContentTags().stream()
                .map(ct -> ct.getTag().getName())
                .collect(Collectors.toSet());
        assertThat(finalTagNames).containsExactlyInAnyOrder("액션", "코미디");
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

        verifyNoInteractions(tagRepository, binaryContentService, eventPublisher, contentMapper);
    }

    @Test
    @DisplayName("수정 시 정규화 후 유효한 태그가 없으면 예외를 던지고 엔티티는 변경되지 않는다")
    void update_allBlankTags_throwsException() {
        // given
        UUID contentId = UUID.randomUUID();
        Content content = Content.createByAdmin(ContentType.MOVIE, "기존 제목", null);
        when(contentRepository.findWithStatsAndTagsById(contentId)).thenReturn(Optional.of(content));

        // when & then
        assertThatThrownBy(() -> contentService.update(
                contentId, new ContentUpdateRequest("수정 제목", null, List.of("  ", " ")), null))
                .isInstanceOf(EmptyTagException.class);

        assertThat(content.getTitle()).isEqualTo("기존 제목");
        verify(tagRepository, never()).findByNameIn(anyList());
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
        // given
        ContentCursorRequest request = new ContentCursorRequest(
                null, null, null, null, null,
                20, Sort.Direction.DESC, ContentSortByType.CREATED_AT
        );
        List<Content> contents = List.of(
                Content.createByAdmin(ContentType.MOVIE, "영화1", null),
                Content.createByAdmin(ContentType.MOVIE, "영화2", null)
        );
        CursorResponse<ContentResponse> expectedResponse = new CursorResponse<>(
                List.of(), null, null, false, 2L, "createdAt", "DESCENDING"
        );

        when(contentRepository.findContents(request, 21)).thenReturn(contents);
        when(contentRepository.countContents(request)).thenReturn(2L);
        when(contentMapper.toCursor(contents, false, 2L, ContentSortByType.CREATED_AT, Sort.Direction.DESC))
                .thenReturn(expectedResponse);

        // when
        CursorResponse<ContentResponse> result = contentService.findContents(request);

        // then
        assertThat(result).isSameAs(expectedResponse);
        verify(contentRepository).findContents(request, 21);
        verify(contentRepository).countContents(request);
        verify(contentMapper).toCursor(contents, false, 2L, ContentSortByType.CREATED_AT, Sort.Direction.DESC);
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
        // given
        ContentCursorRequest request = new ContentCursorRequest(
                ContentType.MOVIE, "존재하지않는키워드", null, null, null,
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
        when(contentRepository.findWithStatsAndTagsById(contentId)).thenReturn(Optional.of(content));

        // when
        contentService.delete(contentId);

        // then
        verify(contentRepository).delete(content);
        verifyNoInteractions(binaryContentService, eventPublisher);
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

    private Tag tagWithId(String name) {
        Tag tag = Tag.create(name);
        ReflectionTestUtils.setField(tag, "id", UUID.randomUUID());
        return tag;
    }
}
