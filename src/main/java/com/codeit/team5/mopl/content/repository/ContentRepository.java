package com.codeit.team5.mopl.content.repository;

import com.codeit.team5.mopl.content.entity.Content;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentRepository extends JpaRepository<Content, UUID> {
}
