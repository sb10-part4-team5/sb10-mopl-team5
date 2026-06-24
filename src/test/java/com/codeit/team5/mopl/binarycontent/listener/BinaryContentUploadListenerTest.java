package com.codeit.team5.mopl.binarycontent.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.binarycontent.BinaryContentStorage;
import com.codeit.team5.mopl.binarycontent.entity.BinaryContentUploadStatus;
import com.codeit.team5.mopl.binarycontent.event.BinaryContentUploadEvent;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.content.exception.ContentNotFoundException;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BinaryContentUploadListenerTest {

    @Mock
    private BinaryContentStorage binaryContentStorage;

    @Mock
    private ContentRepository contentRepository;

    @InjectMocks
    private BinaryContentUploadListener listener;

    @Test
    @DisplayName("파일 업로드 성공 시 COMPLETED 상태로 저장된다")
    void handle_uploadSuccess_completesUpload() {
        // given
        Content content = Content.createByAdmin(ContentType.MOVIE, "테스트", null);
        content.initThumbnail("http://localhost:8080/thumbnails/test.jpg");
        UUID contentId = UUID.randomUUID();
        byte[] bytes = new byte[]{1, 2, 3};
        BinaryContentUploadEvent event = new BinaryContentUploadEvent(contentId, "thumbnails/test.jpg", bytes);

        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

        // when
        listener.handle(event);

        // then
        verify(binaryContentStorage).store("thumbnails/test.jpg", bytes);
        verify(contentRepository).save(content);
        assertThat(content.getThumbnailUploadStatus()).isEqualTo(BinaryContentUploadStatus.COMPLETED);
    }

    @Test
    @DisplayName("파일 업로드 실패 시 FAILED 상태로 저장된다")
    void handle_uploadFails_failsUpload() {
        // given
        Content content = Content.createByAdmin(ContentType.MOVIE, "테스트", null);
        content.initThumbnail("http://localhost:8080/thumbnails/test.jpg");
        UUID contentId = UUID.randomUUID();
        byte[] bytes = new byte[]{1, 2, 3};
        BinaryContentUploadEvent event = new BinaryContentUploadEvent(contentId, "thumbnails/test.jpg", bytes);

        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        doThrow(new RuntimeException("S3 연결 실패")).when(binaryContentStorage).store(any(), any());

        // when
        listener.handle(event);

        // then
        verify(contentRepository).save(content);
        assertThat(content.getThumbnailUploadStatus()).isEqualTo(BinaryContentUploadStatus.FAILED);
    }

    @Test
    @DisplayName("Content가 존재하지 않으면 ContentNotFoundException을 던진다")
    void handle_contentNotFound_throwsException() {
        // given
        UUID contentId = UUID.randomUUID();
        BinaryContentUploadEvent event = new BinaryContentUploadEvent(contentId, "thumbnails/test.jpg", new byte[]{});

        when(contentRepository.findById(contentId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> listener.handle(event))
                .isInstanceOf(ContentNotFoundException.class);

        verify(binaryContentStorage, never()).store(any(), any());
        verify(contentRepository, never()).save(any());
    }
}
