package com.codeit.team5.mopl.review.repository.querydsl;

import com.codeit.team5.mopl.review.entity.Review;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;

public interface ReviewQueryRepository {

    List<Review> findPageByContentId(UUID contentId, String cursor, UUID idAfter,
        Limit limit, String sortBy, boolean ascending);
}
