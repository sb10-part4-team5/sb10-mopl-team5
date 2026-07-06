package com.codeit.team5.mopl.review.repository;

import com.codeit.team5.mopl.review.entity.Review;
import com.codeit.team5.mopl.review.repository.querydsl.ReviewQueryRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, UUID>, ReviewQueryRepository {

    @Query("SELECT r FROM Review r JOIN FETCH r.author WHERE r.id = :id")
    Optional<Review> findByIdWithAuthor(@Param("id") UUID id);

    boolean existsByContent_IdAndAuthor_Id(UUID contentId, UUID authorId);

    long countByContent_Id(UUID contentId);
}
