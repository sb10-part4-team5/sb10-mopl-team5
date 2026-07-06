package com.codeit.team5.mopl.content.service;

import com.codeit.team5.mopl.content.exception.ContentNotFoundException;
import com.codeit.team5.mopl.content.repository.ContentStatsRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ContentStatService {

    private final ContentStatsRepository contentStatsRepository;

    public void reviewUpdateContentStat(UUID contentId, double ratingDelta, int countDelta) {
        int updated = contentStatsRepository.applyStatDelta(contentId, ratingDelta, countDelta);
        if (updated == 0) {
            throw new ContentNotFoundException(contentId);
        }
    }
}
