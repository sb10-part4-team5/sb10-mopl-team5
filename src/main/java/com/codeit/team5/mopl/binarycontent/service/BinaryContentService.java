package com.codeit.team5.mopl.binarycontent.service;

import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.binarycontent.repository.BinaryContentRepository;
import com.codeit.team5.mopl.binarycontent.storage.BinaryContentStorage;
import com.codeit.team5.mopl.binarycontent.storage.GeneratedKey;
import com.codeit.team5.mopl.binarycontent.storage.StorageDirectory;
import com.codeit.team5.mopl.binarycontent.storage.StorageKeyFactory;
import com.codeit.team5.mopl.global.dto.FileRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BinaryContentService {

    private final BinaryContentStorage binaryContentStorage;
    private final StorageKeyFactory storageKeyFactory;
    private final BinaryContentRepository binaryContentRepository;

    @Transactional
    public BinaryContent upload(StorageDirectory directory, UUID ownerId, FileRequest image) {
        GeneratedKey generated = storageKeyFactory.generate(directory, ownerId, image.filename());
        binaryContentStorage.store(generated.key(), image.bytes(), generated.contentType());
        return binaryContentRepository.save(
                BinaryContent.completed(binaryContentStorage.toUrl(generated.key())));
    }
}
