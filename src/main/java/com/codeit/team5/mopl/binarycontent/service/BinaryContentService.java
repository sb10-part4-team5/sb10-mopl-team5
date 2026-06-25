package com.codeit.team5.mopl.binarycontent.service;

import com.codeit.team5.mopl.binarycontent.entity.BinaryContentUploadStatus;
import com.codeit.team5.mopl.binarycontent.exception.BinaryContentNotFoundException;
import com.codeit.team5.mopl.binarycontent.repository.BinaryContentRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BinaryContentService {

    private final BinaryContentRepository binaryContentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateUploadStatus(UUID binaryContentId, BinaryContentUploadStatus status) {
        binaryContentRepository.findById(binaryContentId)
                .orElseThrow(() -> new BinaryContentNotFoundException(binaryContentId))
                .updateUploadStatus(status);
    }
}
