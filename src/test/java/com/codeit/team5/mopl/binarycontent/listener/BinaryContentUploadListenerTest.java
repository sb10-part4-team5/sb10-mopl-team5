package com.codeit.team5.mopl.binarycontent.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.binarycontent.BinaryContentStorage;
import com.codeit.team5.mopl.binarycontent.entity.BinaryContentUploadStatus;
import com.codeit.team5.mopl.binarycontent.event.BinaryContentUploadEvent;
import com.codeit.team5.mopl.content.repository.ContentRepository;
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
    @DisplayName("파일 업로드 성공 시 COMPLETED 상태로 업데이트된다")
    void handle_uploadSuccess_completesUpload() {
        // given
        UUID contentId = UUID.randomUUID();
        byte[] bytes = new byte[]{1, 2, 3};
        BinaryContentUploadEvent event = new BinaryContentUploadEvent(contentId, "thumbnails/test.jpg", bytes);

        // when
        listener.handle(event);

        // then
        verify(binaryContentStorage).store("thumbnails/test.jpg", event.bytes());
        verify(contentRepository).updateThumbnailUploadStatus(contentId, BinaryContentUploadStatus.COMPLETED);
    }

    @Test
    @DisplayName("파일 업로드 실패 시 FAILED 상태로 업데이트된다")
    void handle_uploadFails_failsUpload() {
        // given
        UUID contentId = UUID.randomUUID();
        byte[] bytes = new byte[]{1, 2, 3};
        BinaryContentUploadEvent event = new BinaryContentUploadEvent(contentId, "thumbnails/test.jpg", bytes);

        doThrow(new RuntimeException("S3 연결 실패")).when(binaryContentStorage).store(any(), any());

        // when
        listener.handle(event);

        // then
        verify(contentRepository).updateThumbnailUploadStatus(contentId, BinaryContentUploadStatus.FAILED);
        verify(contentRepository, never()).updateThumbnailUploadStatus(eq(contentId), eq(BinaryContentUploadStatus.COMPLETED));
    }
}
