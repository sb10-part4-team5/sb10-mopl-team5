package com.codeit.team5.mopl.content.store;

import static com.codeit.team5.mopl.global.infra.redis.config.RedisCacheConfig.CONTENT_RATING_STATS_CACHE;

import com.codeit.team5.mopl.content.repository.ContentStatsRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentStatsCacheStore {

    private final ContentStatsRepository contentStatsRepository;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CONTENT_RATING_STATS_CACHE, key = "#contentId", sync = true)
    public ContentRatingStats getRatingStats(UUID contentId) {
        return contentStatsRepository.findById(contentId)
                .map(s -> new ContentRatingStats(s.getReviewCount(), s.getAverageRating()))
                .orElseGet(ContentRatingStats::empty);
    }
}
