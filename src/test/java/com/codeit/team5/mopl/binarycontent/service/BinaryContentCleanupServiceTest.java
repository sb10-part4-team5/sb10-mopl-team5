package com.codeit.team5.mopl.binarycontent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.binarycontent.entity.BinaryContentUploadStatus;
import com.codeit.team5.mopl.binarycontent.repository.BinaryContentRepository;
import com.codeit.team5.mopl.binarycontent.storage.BinaryContentStorage;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BinaryContentCleanupServiceTest {

    @Mock
    private BinaryContentRepository binaryContentRepository;

    @Mock
    private BinaryContentStorage binaryContentStorage;

    @InjectMocks
    private BinaryContentCleanupService binaryContentCleanupService;

    @Test
    @DisplayName("DELETED 대상을 스토리지 → DB 순서로 삭제하고 삭제 건수를 반환한다")
    void cleanUp_deletesStorageThenDbRow() {
        // given
        BinaryContent a = deleted("http://cdn/profiles/a.png");
        BinaryContent b = deleted("http://cdn/profiles/b.png");
        when(binaryContentRepository.findByUploadStatus(BinaryContentUploadStatus.DELETED))
                .thenReturn(List.of(a, b));
        when(binaryContentStorage.extractKey("http://cdn/profiles/a.png"))
                .thenReturn("profiles/a.png");
        when(binaryContentStorage.extractKey("http://cdn/profiles/b.png"))
                .thenReturn("profiles/b.png");

        // when
        int deleted = binaryContentCleanupService.cleanUp();

        // then
        assertThat(deleted).isEqualTo(2);
        // 각 건은 반드시 스토리지 삭제가 DB row 삭제보다 먼저 일어나야 한다.
        InOrder inOrder = inOrder(binaryContentStorage, binaryContentRepository);
        inOrder.verify(binaryContentStorage).delete("profiles/a.png");
        inOrder.verify(binaryContentRepository).delete(a);
        verify(binaryContentStorage).delete("profiles/b.png");
        verify(binaryContentRepository).delete(b);
    }

    @Test
    @DisplayName("스토리지 삭제가 실패한 건은 DB row를 지우지 않고, 나머지는 계속 정리한다")
    void cleanUp_isolatesFailurePerItem() {
        // given
        BinaryContent failing = deleted("http://cdn/profiles/fail.png");
        BinaryContent ok = deleted("http://cdn/profiles/ok.png");
        when(binaryContentRepository.findByUploadStatus(BinaryContentUploadStatus.DELETED))
                .thenReturn(List.of(failing, ok));
        when(binaryContentStorage.extractKey("http://cdn/profiles/fail.png"))
                .thenReturn("profiles/fail.png");
        when(binaryContentStorage.extractKey("http://cdn/profiles/ok.png"))
                .thenReturn("profiles/ok.png");
        doThrow(new RuntimeException("storage down"))
                .when(binaryContentStorage).delete("profiles/fail.png");

        // when
        int deleted = binaryContentCleanupService.cleanUp();

        // then
        assertThat(deleted).isEqualTo(1);
        // 실패 건은 DB에 남아 다음 배치에서 재시도되어야 하므로 delete 호출이 없어야 한다.
        verify(binaryContentRepository, never()).delete(failing);
        verify(binaryContentStorage).delete("profiles/ok.png");
        verify(binaryContentRepository).delete(ok);
    }

    @Test
    @DisplayName("정리 대상이 없으면 스토리지/DB 삭제를 전혀 호출하지 않는다")
    void cleanUp_noTargets() {
        // given
        when(binaryContentRepository.findByUploadStatus(BinaryContentUploadStatus.DELETED))
                .thenReturn(List.of());

        // when
        int deleted = binaryContentCleanupService.cleanUp();

        // then
        assertThat(deleted).isZero();
        verifyNoInteractions(binaryContentStorage);
        verify(binaryContentRepository, never()).delete(any(BinaryContent.class));
    }

    private BinaryContent deleted(String url) {
        BinaryContent binaryContent = BinaryContent.completed(url);
        binaryContent.updateUploadStatus(BinaryContentUploadStatus.DELETED);
        ReflectionTestUtils.setField(binaryContent, "id", UUID.randomUUID());
        return binaryContent;
    }
}
