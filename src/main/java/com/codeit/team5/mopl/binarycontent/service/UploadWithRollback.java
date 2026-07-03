package com.codeit.team5.mopl.binarycontent.service;

import com.codeit.team5.mopl.binarycontent.dto.UploadedBinaryContent;
import com.codeit.team5.mopl.binarycontent.event.BinaryContentDeleteEvent;
import com.codeit.team5.mopl.binarycontent.storage.StorageDirectory;
import com.codeit.team5.mopl.global.dto.FileRequest;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 스토리지 업로드(트랜잭션 밖)를 수행한 뒤 후속 작업을 실행하고,
 * 후속 작업이 실패하면 업로드한 객체를 비동기로 삭제해 롤백한다.
 */
@Component
@RequiredArgsConstructor
public class UploadWithRollback {

    private final BinaryContentService binaryContentService;
    private final ApplicationEventPublisher eventPublisher;

    public <T> T execute(
            StorageDirectory directory,
            FileRequest image,
            Function<UploadedBinaryContent, T> persist
    ) {
        UploadedBinaryContent uploaded = null;
        try {
            if (image != null) {
                uploaded = binaryContentService.uploadToStorage(directory, image);
            }
            return persist.apply(uploaded);
        } catch (RuntimeException e) {
            if (uploaded != null) {
                eventPublisher.publishEvent(new BinaryContentDeleteEvent(uploaded));
            }
            throw e;
        }
    }
}
