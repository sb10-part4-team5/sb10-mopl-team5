package com.codeit.team5.mopl.binarycontent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.binarycontent.dto.UploadedBinaryContent;
import com.codeit.team5.mopl.binarycontent.event.BinaryContentDeleteEvent;
import com.codeit.team5.mopl.binarycontent.storage.StorageDirectory;
import com.codeit.team5.mopl.global.dto.FileRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class UploadWithRollbackTest {

    @Mock
    private BinaryContentService binaryContentService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UploadWithRollback uploadWithRollback;

    private final FileRequest image = new FileRequest(new byte[]{1, 2, 3}, "file.jpg");
    private final UploadedBinaryContent uploaded =
            new UploadedBinaryContent("profiles/key.jpg", "http://localhost/profiles/key.jpg");

    @Test
    @DisplayName("이미지가 없으면 업로드 없이 후속 작업 실행 성공")
    void execute_noImage_skipsUpload() {
        // when
        String result = uploadWithRollback.execute(
                StorageDirectory.PROFILE, null, u -> u == null ? "null" : "not-null");

        // then
        assertThat(result).isEqualTo("null");
        verify(binaryContentService, never()).uploadToStorage(any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("업로드와 후속 작업 성공 시 이벤트 미발행 성공")
    void execute_success_noEvent() {
        // given
        when(binaryContentService.uploadToStorage(eq(StorageDirectory.PROFILE), any())).thenReturn(uploaded);

        // when
        String result = uploadWithRollback.execute(
                StorageDirectory.PROFILE, image, u -> "done:" + u.key());

        // then
        assertThat(result).isEqualTo("done:profiles/key.jpg");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("후속 작업 실패 시 삭제 이벤트 발행 후 예외 전파 성공")
    void execute_persistFails_publishesDeleteEvent() {
        // given
        when(binaryContentService.uploadToStorage(eq(StorageDirectory.PROFILE), any())).thenReturn(uploaded);

        // when & then
        assertThatThrownBy(() -> uploadWithRollback.execute(
                StorageDirectory.PROFILE, image, u -> {
                    throw new RuntimeException("persist fail");
                }))
                .isInstanceOf(RuntimeException.class);

        verify(eventPublisher).publishEvent(new BinaryContentDeleteEvent(uploaded));
    }

    @Test
    @DisplayName("업로드 실패 시 이벤트 미발행하고 예외 전파 성공")
    void execute_uploadFails_noEvent() {
        // given
        when(binaryContentService.uploadToStorage(eq(StorageDirectory.PROFILE), any()))
                .thenThrow(new RuntimeException("upload fail"));

        // when & then
        assertThatThrownBy(() -> uploadWithRollback.execute(
                StorageDirectory.PROFILE, image, u -> "never"))
                .isInstanceOf(RuntimeException.class);

        verify(eventPublisher, never()).publishEvent(any());
    }
}
