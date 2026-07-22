package com.codeit.team5.mopl.binarycontent.repository;

import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.binarycontent.entity.BinaryContentUploadStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BinaryContentRepository extends JpaRepository<BinaryContent, UUID> {

    List<BinaryContent> findByUploadStatus(BinaryContentUploadStatus uploadStatus);
}
