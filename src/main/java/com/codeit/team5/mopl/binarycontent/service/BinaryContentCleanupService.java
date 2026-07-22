package com.codeit.team5.mopl.binarycontent.service;

import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.binarycontent.entity.BinaryContentUploadStatus;
import com.codeit.team5.mopl.binarycontent.repository.BinaryContentRepository;
import com.codeit.team5.mopl.binarycontent.storage.BinaryContentStorage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BinaryContentCleanupService {

    private final BinaryContentRepository binaryContentRepository;
    private final BinaryContentStorage binaryContentStorage;

    public int cleanUp() {
        List<BinaryContent> targets =
                binaryContentRepository.findByUploadStatus(BinaryContentUploadStatus.DELETED);
        int deleted = 0;
        for (BinaryContent target : targets) {
            if (deleteOne(target)) {
                deleted++;
            }
        }
        return deleted;
    }

    private boolean deleteOne(BinaryContent binaryContent) {
        try {
            String key = binaryContentStorage.extractKey(binaryContent.getUrl());
            binaryContentStorage.delete(key);              // 1) 스토리지 객체 먼저 삭제 (멱등)
            binaryContentRepository.delete(binaryContent); // 2) DB row 삭제
            return true;
        } catch (RuntimeException e) {
            // 개별 실패는 다음 배치에서 재시도. 한 건 실패가 전체 배치를 중단시키지 않도록 격리한다.
            log.warn("BinaryContent 정리 실패 (다음 배치에서 재시도): id={}, url={}",
                    binaryContent.getId(), binaryContent.getUrl(), e);
            return false;
        }
    }
}
