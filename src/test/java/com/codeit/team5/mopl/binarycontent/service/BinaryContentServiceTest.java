package com.codeit.team5.mopl.binarycontent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.binarycontent.entity.BinaryContentUploadStatus;
import com.codeit.team5.mopl.binarycontent.repository.BinaryContentRepository;
import com.codeit.team5.mopl.binarycontent.storage.BinaryContentStorage;
import com.codeit.team5.mopl.binarycontent.storage.GeneratedKey;
import com.codeit.team5.mopl.binarycontent.storage.StorageDirectory;
import com.codeit.team5.mopl.binarycontent.storage.StorageKeyFactory;
import com.codeit.team5.mopl.global.dto.FileRequest;
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
    private BinaryContentStorage binaryContentStorage;

    @Mock
    private StorageKeyFactory storageKeyFactory;

    @Mock
    private BinaryContentRepository binaryContentRepository;

    @InjectMocks
    private BinaryContentService binaryContentService;

    @Test
    @DisplayName("이미지 업로드 후 COMPLETED 상태로 저장에 성공한다")
    void upload_success() {
        // given
        UUID ownerId = UUID.randomUUID();
        FileRequest image = new FileRequest(new byte[]{1, 2, 3}, "profile.jpg");
        when(storageKeyFactory.generate(eq(StorageDirectory.PROFILE), eq(ownerId), eq("profile.jpg")))
                .thenReturn(new GeneratedKey("profiles/key.jpg", "image/jpeg"));
        when(binaryContentStorage.toUrl("profiles/key.jpg"))
                .thenReturn("http://localhost/profiles/key.jpg");
        when(binaryContentRepository.save(any(BinaryContent.class))).then(returnsFirstArg());

        // when
        BinaryContent result = binaryContentService.upload(StorageDirectory.PROFILE, ownerId, image);

        // then
        verify(binaryContentStorage).store("profiles/key.jpg", image.bytes(), "image/jpeg");
        assertThat(result.getUrl()).isEqualTo("http://localhost/profiles/key.jpg");
        assertThat(result.getUploadStatus()).isEqualTo(BinaryContentUploadStatus.COMPLETED);
    }

    @Test
    @DisplayName("스토리지 업로드 실패 시 예외를 전파하고 저장하지 않는다")
    void upload_storeFails_throwsAndDoesNotSave() {
        // given
        UUID ownerId = UUID.randomUUID();
        FileRequest image = new FileRequest(new byte[]{1, 2, 3}, "profile.jpg");
        when(storageKeyFactory.generate(eq(StorageDirectory.PROFILE), eq(ownerId), eq("profile.jpg")))
                .thenReturn(new GeneratedKey("profiles/key.jpg", "image/jpeg"));
        doThrow(new RuntimeException("S3 연결 실패"))
                .when(binaryContentStorage).store(any(), any(), any());

        // when & then
        assertThatThrownBy(() -> binaryContentService.upload(StorageDirectory.PROFILE, ownerId, image))
                .isInstanceOf(RuntimeException.class);
        verify(binaryContentRepository, never()).save(any());
    }
}
