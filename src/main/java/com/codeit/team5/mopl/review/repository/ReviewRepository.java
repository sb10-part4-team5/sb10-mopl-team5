package com.codeit.team5.mopl.review.repository;

import com.codeit.team5.mopl.review.entity.Review;
import com.codeit.team5.mopl.review.repository.querydsl.ReviewQueryRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, UUID>, ReviewQueryRepository {

    boolean existsByContentIdAndAuthorId(UUID contentId, UUID authorId);

    long countByContentId(UUID contentId);
}
