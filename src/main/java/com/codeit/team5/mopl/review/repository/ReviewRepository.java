package com.codeit.team5.mopl.review.repository;

import com.codeit.team5.mopl.review.entity.Review;
import com.codeit.team5.mopl.review.repository.querydsl.ReviewQueryRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, UUID>, ReviewQueryRepository {

    boolean existsByContent_IdAndAuthorId(UUID contentId, UUID authorId);

    long countByContent_Id(UUID contentId);
}
