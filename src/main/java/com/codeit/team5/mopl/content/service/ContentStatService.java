package com.codeit.team5.mopl.content.service;

import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentStats;
import com.codeit.team5.mopl.content.exception.ContentNotFoundException;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ContentStatService {

    private final ContentRepository contentRepository;

    public void updateContentStat(UUID contentId, double ratingDelta, int countDelta) {
        Content content = contentRepository.findById(contentId).orElseThrow(() ->
            new ContentNotFoundException(contentId));

        ContentStats stats = content.getStats();
        int newCount = stats.getReviewCount() + countDelta;
        double newRatingSum = stats.getRatingSum() + ratingDelta;
        stats.updateRating(newCount == 0 ? 0.0 : newRatingSum, newCount);
    }

}
