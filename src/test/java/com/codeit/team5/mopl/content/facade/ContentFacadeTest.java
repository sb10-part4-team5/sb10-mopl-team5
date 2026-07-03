package com.codeit.team5.mopl.content.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.binarycontent.dto.UploadedBinaryContent;
import com.codeit.team5.mopl.binarycontent.service.BinaryContentService;
import com.codeit.team5.mopl.binarycontent.event.BinaryContentDeleteEvent;
import com.codeit.team5.mopl.binarycontent.service.UploadWithRollback;
import com.codeit.team5.mopl.binarycontent.storage.StorageDirectory;
import com.codeit.team5.mopl.content.dto.request.ContentCreateRequest;
import com.codeit.team5.mopl.content.dto.request.ContentUpdateRequest;
import com.codeit.team5.mopl.content.dto.response.ContentResponse;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.content.service.ContentService;
import com.codeit.team5.mopl.global.dto.FileRequest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentFacadeTest {

    @Mock
    private ContentService contentService;

    @Mock
    private BinaryContentService binaryContentService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ContentFacade contentFacade;

    @BeforeEach
    void setUp() {
        contentFacade = new ContentFacade(
                contentService, new UploadWithRollback(binaryContentService, eventPublisher));
    }

    private final ContentCreateRequest createRequest =
            new ContentCreateRequest(ContentType.MOVIE, "제목", "설명", List.of("액션"));
    private final ContentUpdateRequest updateRequest =
            new ContentUpdateRequest("제목", "설명", List.of("액션"));
    private final FileRequest image = new FileRequest(new byte[]{1, 2, 3}, "thumb.jpg");
    private final UploadedBinaryContent uploaded =
            new UploadedBinaryContent("thumbnails/key.jpg", "http://localhost/thumbnails/key.jpg");

    private ContentResponse response() {
        return new ContentResponse(UUID.randomUUID(), ContentType.MOVIE, "제목", "설명",
                null, List.of("액션"), 0.0, 0, 0);
    }

    @Test
    @DisplayName("이미지가 있으면 스토리지 업로드 후 생성 성공")
    void create_withImage_success() {
        // given
        ContentResponse expected = response();
        when(binaryContentService.uploadToStorage(eq(StorageDirectory.THUMBNAIL), any())).thenReturn(uploaded);
        when(contentService.create(eq(createRequest), eq(uploaded))).thenReturn(expected);

        // when
        ContentResponse result = contentFacade.create(createRequest, image);

        // then
        assertThat(result).isSameAs(expected);
        verify(binaryContentService).uploadToStorage(eq(StorageDirectory.THUMBNAIL), any());
        verify(eventPublisher, never()).publishEvent(any(BinaryContentDeleteEvent.class));
    }

    @Test
    @DisplayName("이미지가 없으면 업로드 없이 생성 성공")
    void create_noImage_success() {
        // given
        ContentResponse expected = response();
        when(contentService.create(eq(createRequest), isNull())).thenReturn(expected);

        // when
        ContentResponse result = contentFacade.create(createRequest, null);

        // then
        assertThat(result).isSameAs(expected);
        verify(binaryContentService, never()).uploadToStorage(any(), any());
    }

    @Test
    @DisplayName("생성 실패 시 보상 삭제 후 예외 전파 성공")
    void create_serviceFails_compensates() {
        // given
        when(binaryContentService.uploadToStorage(eq(StorageDirectory.THUMBNAIL), any())).thenReturn(uploaded);
        when(contentService.create(eq(createRequest), eq(uploaded)))
                .thenThrow(new RuntimeException("db failure"));

        // when & then
        assertThatThrownBy(() -> contentFacade.create(createRequest, image))
                .isInstanceOf(RuntimeException.class);
        verify(eventPublisher).publishEvent(new BinaryContentDeleteEvent(uploaded));
    }

    @Test
    @DisplayName("이미지가 있으면 스토리지 업로드 후 수정 성공")
    void update_withImage_success() {
        // given
        UUID contentId = UUID.randomUUID();
        ContentResponse expected = response();
        when(binaryContentService.uploadToStorage(eq(StorageDirectory.THUMBNAIL), any())).thenReturn(uploaded);
        when(contentService.update(eq(contentId), eq(updateRequest), eq(uploaded))).thenReturn(expected);

        // when
        ContentResponse result = contentFacade.update(contentId, updateRequest, image);

        // then
        assertThat(result).isSameAs(expected);
        verify(eventPublisher, never()).publishEvent(any(BinaryContentDeleteEvent.class));
    }

    @Test
    @DisplayName("수정 실패 시 보상 삭제 후 예외 전파 성공")
    void update_serviceFails_compensates() {
        // given
        UUID contentId = UUID.randomUUID();
        when(binaryContentService.uploadToStorage(eq(StorageDirectory.THUMBNAIL), any())).thenReturn(uploaded);
        when(contentService.update(eq(contentId), eq(updateRequest), eq(uploaded)))
                .thenThrow(new RuntimeException("db failure"));

        // when & then
        assertThatThrownBy(() -> contentFacade.update(contentId, updateRequest, image))
                .isInstanceOf(RuntimeException.class);
        verify(eventPublisher).publishEvent(new BinaryContentDeleteEvent(uploaded));
    }
}
