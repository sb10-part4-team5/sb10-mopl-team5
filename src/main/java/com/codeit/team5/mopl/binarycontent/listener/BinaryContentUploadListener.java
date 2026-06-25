package com.codeit.team5.mopl.binarycontent.listener;

import com.codeit.team5.mopl.binarycontent.BinaryContentStorage;
import com.codeit.team5.mopl.binarycontent.event.BinaryContentUploadEvent;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.exception.ContentNotFoundException;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
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
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(BinaryContentUploadEvent event) {
        Content content = contentRepository.findById(event.contentId())
                .orElseThrow(() -> {
                    log.error("파일 업로드 대상 콘텐츠를 찾을 수 없음 - contentId: {}", event.contentId());
                    return new ContentNotFoundException();
                });
        try {
            binaryContentStorage.store(event.key(), event.bytes());
            content.completeThumbnailUpload();
            log.debug("파일 업로드 완료 - contentId: {}", event.contentId());
        } catch (Exception e) {
            content.failThumbnailUpload();
            log.error("파일 업로드 실패 - contentId: {}", event.contentId(), e);
        } finally {
            contentRepository.save(content);
        }
    }
}
