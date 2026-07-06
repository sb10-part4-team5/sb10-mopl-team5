package com.codeit.team5.mopl.binarycontent.service;

import com.codeit.team5.mopl.binarycontent.dto.UploadedBinaryContent;
import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.binarycontent.repository.BinaryContentRepository;
import com.codeit.team5.mopl.binarycontent.storage.BinaryContentStorage;
import com.codeit.team5.mopl.binarycontent.storage.GeneratedKey;
import com.codeit.team5.mopl.binarycontent.storage.StorageDirectory;
import com.codeit.team5.mopl.binarycontent.storage.StorageKeyFactory;
import com.codeit.team5.mopl.global.dto.FileRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BinaryContentService {

    private final BinaryContentStorage binaryContentStorage;
    private final StorageKeyFactory storageKeyFactory;
    private final BinaryContentRepository binaryContentRepository;

    public UploadedBinaryContent uploadToStorage(StorageDirectory directory, FileRequest image) {
        GeneratedKey generated = storageKeyFactory.generate(directory, image.filename());
        binaryContentStorage.store(generated.key(), image.bytes(), generated.contentType());
        return new UploadedBinaryContent(generated.key(), binaryContentStorage.toUrl(generated.key()));
    }

    @Transactional
    public BinaryContent saveCompleted(UploadedBinaryContent uploaded) {
        return binaryContentRepository.save(BinaryContent.completed(uploaded.url()));
    }

    public void deleteQuietly(UploadedBinaryContent uploaded) {
        if (uploaded == null) {
            return;
        }
        try {
            binaryContentStorage.delete(uploaded.key());
        } catch (RuntimeException e) {
            log.warn("업로드 보상 삭제 실패: key={}", uploaded.key(), e);
        }
    }
}
