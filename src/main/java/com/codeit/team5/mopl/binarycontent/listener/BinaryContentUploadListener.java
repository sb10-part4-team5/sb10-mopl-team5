package com.codeit.team5.mopl.binarycontent.listener;

import com.codeit.team5.mopl.binarycontent.BinaryContentStorage;
import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.binarycontent.event.BinaryContentUploadEvent;
import com.codeit.team5.mopl.binarycontent.repository.BinaryContentRepository;
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
    private final BinaryContentRepository binaryContentRepository;

    @Async("binaryContentUploadExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(BinaryContentUploadEvent event) {
        BinaryContent binaryContent = binaryContentRepository.findById(event.binaryContentId())
                .orElseThrow(() -> new IllegalStateException("BinaryContent를 찾을 수 없습니다: " + event.binaryContentId()));
        try {
            binaryContentStorage.store(event.key(), event.bytes());
            binaryContent.completeUpload();
            log.debug("파일 업로드 완료 - binaryContentId: {}", event.binaryContentId());
        } catch (Exception e) {
            binaryContent.failUpload();
            log.error("파일 업로드 실패 - binaryContentId: {}", event.binaryContentId(), e);
        }
    }
}
