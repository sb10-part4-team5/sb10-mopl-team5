package com.codeit.team5.mopl.content.entity;

import com.codeit.team5.mopl.content.exception.InvalidContentStatsException;
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

    public double getAverageRating() {
        if (reviewCount == 0) return 0.0;
        return ratingSum / reviewCount;
    }

    public void updateRating(double newRatingSum, int newReviewCount) {
        if (newReviewCount < 0) {
            throw new InvalidContentStatsException("reviewCount는 음수일 수 없습니다.");
        }
        if (!Double.isFinite(newRatingSum) || newRatingSum < 0) {
            throw new InvalidContentStatsException("ratingSum은 음수이거나 비정상 값(NaN, Infinity)일 수 없습니다.");
        }
        if (newReviewCount == 0 && newRatingSum != 0) {
            throw new InvalidContentStatsException("reviewCount가 0이면 ratingSum도 0이어야 합니다.");
        }
        this.ratingSum = newRatingSum;
        this.reviewCount = newReviewCount;
    }
}
