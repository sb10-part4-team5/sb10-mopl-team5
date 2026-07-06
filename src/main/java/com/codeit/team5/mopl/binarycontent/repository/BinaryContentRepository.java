package com.codeit.team5.mopl.binarycontent.repository;

import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BinaryContentRepository extends JpaRepository<BinaryContent, UUID> {
}
