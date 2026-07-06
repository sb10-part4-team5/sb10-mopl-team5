package com.codeit.team5.mopl.content.repository;

import com.codeit.team5.mopl.content.entity.ContentStats;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentStatsRepository extends JpaRepository<ContentStats, UUID> {
}
