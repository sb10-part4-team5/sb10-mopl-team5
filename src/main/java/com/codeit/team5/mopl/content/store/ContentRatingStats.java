package com.codeit.team5.mopl.content.store;

// ContentStat 캐싱을 위해 reviewCount, averageRating만 담는 캐싱용 DTO
// watchingCount는 실시간 성이 강해 캐싱 제외
public record ContentRatingStats(
        int reviewCount,
        double averageRating
) {
    public static ContentRatingStats empty() {
        return new ContentRatingStats(0, 0.0);
    }
}
