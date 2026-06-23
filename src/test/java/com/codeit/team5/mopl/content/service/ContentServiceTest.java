package com.codeit.team5.mopl.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.content.dto.request.ContentCreateRequest;
import com.codeit.team5.mopl.content.dto.response.ContentResponse;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentStats;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.content.mapper.ContentMapper;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.content.repository.ContentStatsRepository;
import com.codeit.team5.mopl.tag.entity.Tag;
import com.codeit.team5.mopl.tag.repository.TagRepository;
import java.util.List;
import java.util.Optional;
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
        when(tagRepository.findByName("액션")).thenReturn(Optional.of(actionTag));
        when(tagRepository.findByName("드라마")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenReturn(dramaTag);
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

        verify(tagRepository).findByName("액션");
        verify(tagRepository).findByName("드라마");
        verify(tagRepository, times(1)).save(any(Tag.class));
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
        when(tagRepository.findByName("로맨스")).thenReturn(Optional.of(romanceTag));
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
        when(tagRepository.findByName("액션")).thenReturn(Optional.of(existingTag));
        when(contentStatsRepository.save(any(ContentStats.class))).then(returnsFirstArg());
        when(contentMapper.toDto(any(Content.class), any(), any(ContentStats.class))).thenReturn(any());

        // When
        contentService.create(request, null);

        // Then
        verify(tagRepository, never()).save(any(Tag.class));
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
        when(tagRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).then(returnsFirstArg());
        when(contentStatsRepository.save(any(ContentStats.class))).then(returnsFirstArg());
        when(contentMapper.toDto(any(Content.class), any(), any(ContentStats.class))).thenReturn(any());

        // When
        contentService.create(request, null);

        // Then
        verify(tagRepository, times(2)).save(any(Tag.class));
    }
}
