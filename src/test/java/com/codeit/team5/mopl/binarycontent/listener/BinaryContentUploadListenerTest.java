package com.codeit.team5.mopl.binarycontent.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.binarycontent.BinaryContentStorage;
import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.binarycontent.entity.BinaryContentUploadStatus;
import com.codeit.team5.mopl.binarycontent.event.BinaryContentUploadEvent;
import com.codeit.team5.mopl.binarycontent.repository.BinaryContentRepository;
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
    private BinaryContentRepository binaryContentRepository;

    @InjectMocks
    private BinaryContentUploadListener listener;

    @Test
    @DisplayName("파일 업로드 성공 시 COMPLETED 상태로 업데이트된다")
    void handle_uploadSuccess_completesUpload() {
        // given
        UUID binaryContentId = UUID.randomUUID();
        byte[] bytes = new byte[]{1, 2, 3};
        BinaryContentUploadEvent event = new BinaryContentUploadEvent(binaryContentId, "thumbnails/test.jpg", bytes);
        BinaryContent binaryContent = BinaryContent.pending("http://localhost:8080/thumbnails/test.jpg");
        when(binaryContentRepository.findById(binaryContentId)).thenReturn(Optional.of(binaryContent));

        // when
        listener.handle(event);

        // then
        verify(binaryContentStorage).store("thumbnails/test.jpg", event.bytes());
        assertThat(binaryContent.getUploadStatus()).isEqualTo(BinaryContentUploadStatus.COMPLETED);
    }

    @Test
    @DisplayName("파일 업로드 실패 시 FAILED 상태로 업데이트된다")
    void handle_uploadFails_failsUpload() {
        // given
        UUID binaryContentId = UUID.randomUUID();
        byte[] bytes = new byte[]{1, 2, 3};
        BinaryContentUploadEvent event = new BinaryContentUploadEvent(binaryContentId, "thumbnails/test.jpg", bytes);
        BinaryContent binaryContent = BinaryContent.pending("http://localhost:8080/thumbnails/test.jpg");
        when(binaryContentRepository.findById(binaryContentId)).thenReturn(Optional.of(binaryContent));
        doThrow(new RuntimeException("S3 연결 실패")).when(binaryContentStorage).store(any(), any());

        // when
        listener.handle(event);

        // then
        assertThat(binaryContent.getUploadStatus()).isEqualTo(BinaryContentUploadStatus.FAILED);
    }
}
