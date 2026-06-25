package com.codeit.team5.mopl.content.entity;

import com.codeit.team5.mopl.global.entity.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "content_stats")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentStats extends BaseUpdatableEntity {

    @Column(nullable = false)
    private int reviewCount;

    @Column(nullable = false)
    private double ratingSum;

    @Column(nullable = false)
    private long watcherCount;

    public static ContentStats create() {
        ContentStats stats = new ContentStats();
        stats.reviewCount = 0;
        stats.ratingSum = 0.0;
        stats.watcherCount = 0;
        return stats;
    }
}
