package com.codeit.team5.mopl.review.repository.querydsl;

import com.codeit.team5.mopl.review.entity.Review;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;

public interface ReviewQueryRepository {

    List<Review> findPageByContentIdSortByCreatedAt(UUID contentId, Instant cursor, UUID idAfter, Limit limit, Sort.Direction sortDirection);

    List<Review> findPageByContentIdSortByRating(UUID contentId, Double cursor, UUID idAfter, Limit limit, Sort.Direction sortDirection);

}
