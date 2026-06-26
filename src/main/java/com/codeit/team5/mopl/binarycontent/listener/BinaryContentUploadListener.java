package com.codeit.team5.mopl.binarycontent.listener;

import com.codeit.team5.mopl.binarycontent.storage.BinaryContentStorage;
import com.codeit.team5.mopl.binarycontent.entity.BinaryContentUploadStatus;
import com.codeit.team5.mopl.binarycontent.event.BinaryContentUploadEvent;
import com.codeit.team5.mopl.binarycontent.service.BinaryContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class BinaryContentUploadListener {

    private final BinaryContentStorage binaryContentStorage;
    private final BinaryContentService binaryContentService;

    @Async("binaryContentUploadExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(BinaryContentUploadEvent event) {
        try {
            binaryContentStorage.store(event.key(), event.bytes(), event.contentType());
            binaryContentService.updateUploadStatus(event.binaryContentId(), BinaryContentUploadStatus.COMPLETED);
            log.debug("파일 업로드 완료 - binaryContentId: {}", event.binaryContentId());
        } catch (Exception e) {
            binaryContentService.updateUploadStatus(event.binaryContentId(), BinaryContentUploadStatus.FAILED);
            log.error("파일 업로드 실패 - binaryContentId: {}", event.binaryContentId(), e);
        }
    }
}
