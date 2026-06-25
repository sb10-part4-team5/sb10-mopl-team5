package com.codeit.team5.mopl.binarycontent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.binarycontent.entity.BinaryContentUploadStatus;
import com.codeit.team5.mopl.binarycontent.exception.BinaryContentNotFoundException;
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
class BinaryContentServiceTest {

    @Mock
    private BinaryContentRepository binaryContentRepository;

    @InjectMocks
    private BinaryContentService binaryContentService;

    @Test
    @DisplayName("COMPLETED 상태로 업데이트한다")
    void updateUploadStatus_completed() {
        // given
        UUID id = UUID.randomUUID();
        BinaryContent binaryContent = BinaryContent.pending("http://localhost/test.jpg");
        when(binaryContentRepository.findById(id)).thenReturn(Optional.of(binaryContent));

        // when
        binaryContentService.updateUploadStatus(id, BinaryContentUploadStatus.COMPLETED);

        // then
        assertThat(binaryContent.getUploadStatus()).isEqualTo(BinaryContentUploadStatus.COMPLETED);
    }

    @Test
    @DisplayName("FAILED 상태로 업데이트한다")
    void updateUploadStatus_failed() {
        // given
        UUID id = UUID.randomUUID();
        BinaryContent binaryContent = BinaryContent.pending("http://localhost/test.jpg");
        when(binaryContentRepository.findById(id)).thenReturn(Optional.of(binaryContent));

        // when
        binaryContentService.updateUploadStatus(id, BinaryContentUploadStatus.FAILED);

        // then
        assertThat(binaryContent.getUploadStatus()).isEqualTo(BinaryContentUploadStatus.FAILED);
    }

    @Test
    @DisplayName("BinaryContent가 없으면 BinaryContentNotFoundException을 던진다")
    void updateUploadStatus_notFound_throwsException() {
        // given
        UUID id = UUID.randomUUID();
        when(binaryContentRepository.findById(id)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> binaryContentService.updateUploadStatus(id, BinaryContentUploadStatus.COMPLETED))
                .isInstanceOf(BinaryContentNotFoundException.class);
    }
}
