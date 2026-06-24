package com.codeit.team5.mopl.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.content.dto.request.ContentCreateRequest;
import com.codeit.team5.mopl.content.dto.response.ContentResponse;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentStats;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.content.exception.ContentException;
import com.codeit.team5.mopl.content.exception.EmptyTagException;
import com.codeit.team5.mopl.content.mapper.ContentMapper;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.content.repository.ContentStatsRepository;
import com.codeit.team5.mopl.tag.entity.Tag;
import com.codeit.team5.mopl.tag.repository.TagRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

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

    @InjectMocks
    private ContentService contentService;

    @Test
    @DisplayName("콘텐츠 생성에 성공한다")
    void create_success() {
        // Given
        ContentCreateRequest request = new ContentCreateRequest(
                ContentType.MOVIE,
                "테스트 영화",
                "테스트 설명",
                List.of("액션", "드라마")
        );
        MultipartFile thumbnail = new MockMultipartFile("thumbnail", "test.jpg", "image/jpeg", new byte[]{1, 2, 3});

        Tag actionTag = Tag.create("액션");
        Tag dramaTag = Tag.create("드라마");
        ContentResponse expectedResponse = new ContentResponse(
                UUID.randomUUID(),
                ContentType.MOVIE,
                "테스트 영화",
                "테스트 설명",
                null,
                List.of("액션", "드라마"),
                0.0, 0, 0
        );

        when(contentRepository.save(any(Content.class))).then(returnsFirstArg());
        when(tagRepository.findByNameIn(List.of("액션", "드라마"))).thenReturn(List.of(actionTag));
        when(tagRepository.saveAll(anyList())).thenReturn(List.of(dramaTag));
        when(contentStatsRepository.save(any(ContentStats.class))).then(returnsFirstArg());
        when(contentMapper.toDto(any(Content.class), any(), any(ContentStats.class))).thenReturn(expectedResponse);

        // When
        ContentResponse result = contentService.create(request, thumbnail);

        // Then
        assertThat(result).isSameAs(expectedResponse);

        ArgumentCaptor<Content> contentCaptor = ArgumentCaptor.forClass(Content.class);
        verify(contentRepository).save(contentCaptor.capture());
        Content savedContent = contentCaptor.getValue();
        assertThat(savedContent.getType()).isEqualTo(ContentType.MOVIE);
        assertThat(savedContent.getTitle()).isEqualTo("테스트 영화");
        assertThat(savedContent.getDescription()).isEqualTo("테스트 설명");

        verify(tagRepository).findByNameIn(List.of("액션", "드라마"));
        ArgumentCaptor<List<Tag>> saveAllCaptor = ArgumentCaptor.forClass(List.class);
        verify(tagRepository).saveAll(saveAllCaptor.capture());
        assertThat(saveAllCaptor.getValue()).hasSize(1);
        verify(contentStatsRepository).save(any(ContentStats.class));
    }

    @Test
    @DisplayName("thumbnail이 null이어도 콘텐츠 생성에 성공한다")
    void create_withoutThumbnail_success() {
        // Given
        ContentCreateRequest request = new ContentCreateRequest(
                ContentType.TV_SERIES,
                "테스트 드라마",
                null,
                List.of("로맨스")
        );
        Tag romanceTag = Tag.create("로맨스");
        ContentResponse expectedResponse = new ContentResponse(
                UUID.randomUUID(),
                ContentType.TV_SERIES,
                "테스트 드라마",
                null,
                null,
                List.of("로맨스"),
                0.0, 0, 0
        );

        when(contentRepository.save(any(Content.class))).then(returnsFirstArg());
        when(tagRepository.findByNameIn(List.of("로맨스"))).thenReturn(List.of(romanceTag));
        when(contentStatsRepository.save(any(ContentStats.class))).then(returnsFirstArg());
        when(contentMapper.toDto(any(Content.class), any(), any(ContentStats.class))).thenReturn(expectedResponse);

        // When
        ContentResponse result = contentService.create(request, null);

        // Then
        assertThat(result).isSameAs(expectedResponse);
        verify(contentRepository).save(any(Content.class));
        verify(contentStatsRepository).save(any(ContentStats.class));
    }

    @Test
    @DisplayName("이미 존재하는 태그는 새로 저장하지 않는다")
    void create_existingTag_doesNotSaveNewTag() {
        // Given
        ContentCreateRequest request = new ContentCreateRequest(
                ContentType.MOVIE,
                "테스트 영화",
                null,
                List.of("액션")
        );
        Tag existingTag = Tag.create("액션");

        when(contentRepository.save(any(Content.class))).then(returnsFirstArg());
        when(tagRepository.findByNameIn(List.of("액션"))).thenReturn(List.of(existingTag));
        when(contentStatsRepository.save(any(ContentStats.class))).then(returnsFirstArg());
        when(contentMapper.toDto(any(Content.class), any(), any(ContentStats.class))).thenReturn(null);

        // When
        contentService.create(request, null);

        // Then
        verify(tagRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("존재하지 않는 태그는 새로 저장한다")
    void create_newTag_savesTag() {
        // Given
        ContentCreateRequest request = new ContentCreateRequest(
                ContentType.MOVIE,
                "테스트 영화",
                null,
                List.of("새태그1", "새태그2")
        );

        when(contentRepository.save(any(Content.class))).then(returnsFirstArg());
        when(tagRepository.findByNameIn(anyList())).thenReturn(List.of());
        when(tagRepository.saveAll(anyList())).then(returnsFirstArg());
        when(contentStatsRepository.save(any(ContentStats.class))).then(returnsFirstArg());
        when(contentMapper.toDto(any(Content.class), any(), any(ContentStats.class))).thenReturn(null);

        // When
        contentService.create(request, null);

        // Then
        ArgumentCaptor<List<Tag>> saveAllCaptor = ArgumentCaptor.forClass(List.class);
        verify(tagRepository).saveAll(saveAllCaptor.capture());
        assertThat(saveAllCaptor.getValue()).hasSize(2);
    }

    @Test
    @DisplayName("중복된 태그가 요청에 포함되어도 한 번만 저장한다")
    void create_duplicateTags_savesOnce() {
        // Given
        ContentCreateRequest request = new ContentCreateRequest(
                ContentType.MOVIE,
                "테스트 영화",
                null,
                List.of("액션", "액션")
        );

        when(contentRepository.save(any(Content.class))).then(returnsFirstArg());
        when(tagRepository.findByNameIn(List.of("액션"))).thenReturn(List.of());
        when(tagRepository.saveAll(anyList())).then(returnsFirstArg());
        when(contentStatsRepository.save(any(ContentStats.class))).then(returnsFirstArg());
        when(contentMapper.toDto(any(Content.class), any(), any(ContentStats.class))).thenReturn(null);

        // When
        contentService.create(request, null);

        // Then
        ArgumentCaptor<List<Tag>> saveAllCaptor = ArgumentCaptor.forClass(List.class);
        verify(tagRepository).saveAll(saveAllCaptor.capture());
        assertThat(saveAllCaptor.getValue()).hasSize(1);
    }

    @Test
    @DisplayName("정규화 후 유효한 태그가 없으면 예외를 던진다")
    void create_allBlankTags_throwsException() {
        // Given
        ContentCreateRequest request = new ContentCreateRequest(
                ContentType.MOVIE,
                "테스트 영화",
                null,
                List.of("   ", " ")
        );

        when(contentRepository.save(any(Content.class))).then(returnsFirstArg());

        // When & Then
        assertThatThrownBy(() -> contentService.create(request, null))
                .isInstanceOf(EmptyTagException.class);

        verify(tagRepository, never()).findByNameIn(anyList());
        verify(tagRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("태그는 앞뒤 공백 제거 및 소문자로 정규화되어 저장된다")
    void create_tagsNormalized() {
        // Given
        ContentCreateRequest request = new ContentCreateRequest(
                ContentType.MOVIE,
                "테스트 영화",
                null,
                List.of("  Action  ", "DRAMA")
        );

        when(contentRepository.save(any(Content.class))).then(returnsFirstArg());
        when(tagRepository.findByNameIn(List.of("action", "drama"))).thenReturn(List.of());
        when(tagRepository.saveAll(anyList())).then(returnsFirstArg());
        when(contentStatsRepository.save(any(ContentStats.class))).then(returnsFirstArg());
        when(contentMapper.toDto(any(Content.class), any(), any(ContentStats.class))).thenReturn(null);

        // When
        contentService.create(request, null);

        // Then
        verify(tagRepository).findByNameIn(List.of("action", "drama"));
    }
}
