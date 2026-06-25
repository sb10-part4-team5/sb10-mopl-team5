package com.codeit.team5.mopl.binarycontent.listener;

import com.codeit.team5.mopl.binarycontent.BinaryContentStorage;
import com.codeit.team5.mopl.binarycontent.entity.BinaryContentUploadStatus;
import com.codeit.team5.mopl.binarycontent.event.BinaryContentUploadEvent;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class BinaryContentUploadListener {

    private final BinaryContentStorage binaryContentStorage;
    private final ContentRepository contentRepository;

    @Async("binaryContentUploadExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(BinaryContentUploadEvent event) {
        try {
            binaryContentStorage.store(event.key(), event.bytes());
            contentRepository.updateThumbnailUploadStatus(event.contentId(), BinaryContentUploadStatus.COMPLETED);
            log.debug("파일 업로드 완료 - contentId: {}", event.contentId());
        } catch (Exception e) {
            contentRepository.updateThumbnailUploadStatus(event.contentId(), BinaryContentUploadStatus.FAILED);
            log.error("파일 업로드 실패 - contentId: {}", event.contentId(), e);
        }
    }
}
