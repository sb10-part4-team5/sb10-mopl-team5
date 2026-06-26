package com.codeit.team5.mopl.binarycontent.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.binarycontent.storage.BinaryContentStorage;
import com.codeit.team5.mopl.binarycontent.entity.BinaryContentUploadStatus;
import com.codeit.team5.mopl.binarycontent.event.BinaryContentUploadEvent;
import com.codeit.team5.mopl.binarycontent.service.BinaryContentService;
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
    private BinaryContentService binaryContentService;

    @InjectMocks
    private BinaryContentUploadListener listener;

    @Test
    @DisplayName("파일 업로드 성공 시 COMPLETED 상태로 업데이트한다")
    void handle_uploadSuccess_updatesCompleted() {
        // given
        UUID binaryContentId = UUID.randomUUID();
        BinaryContentUploadEvent event = new BinaryContentUploadEvent(binaryContentId, "thumbnails/test.jpg", new byte[]{1, 2, 3}, "image/jpeg");

        // when
        listener.handle(event);

        // then
        verify(binaryContentStorage).store("thumbnails/test.jpg", event.bytes(), "image/jpeg");
        verify(binaryContentService).updateUploadStatus(binaryContentId, BinaryContentUploadStatus.COMPLETED);
    }

    @Test
    @DisplayName("파일 업로드 실패 시 FAILED 상태로 업데이트한다")
    void handle_uploadFails_updatesFailed() {
        // given
        UUID binaryContentId = UUID.randomUUID();
        BinaryContentUploadEvent event = new BinaryContentUploadEvent(binaryContentId, "thumbnails/test.jpg", new byte[]{1, 2, 3}, "image/jpeg");
        doThrow(new RuntimeException("S3 연결 실패")).when(binaryContentStorage).store(any(), any(), any());

        // when
        listener.handle(event);

        // then
        verify(binaryContentService).updateUploadStatus(binaryContentId, BinaryContentUploadStatus.FAILED);
    }
}
