package com.codeit.team5.mopl.review.entity;

import com.codeit.team5.mopl.global.entity.BaseEntity;
import com.codeit.team5.mopl.global.entity.BaseUpdatableEntity;
import com.codeit.team5.mopl.review.exception.InvalidRatingException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reviews",
    uniqueConstraints = @UniqueConstraint(columnNames = {"content_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseUpdatableEntity {

    @Column(name = "content_id", nullable = false, columnDefinition = "uuid")
    private UUID contentId;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID authorId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    @Column(nullable = false)
    private Double rating;

    public static Review create(UUID contentId, UUID authorId, String text, Double rating) {
        validateRating(rating);
        return new Review(contentId, authorId, text, rating);
    }

    private Review(UUID contentId, UUID authorId, String text, Double rating) {
        this.contentId = contentId;
        this.authorId = authorId;
        this.text = text;
        this.rating = rating;
    }

    public void update(String text, Double rating) {
        validateRating(rating);
        if (text != null) {
            this.text = text;
        }
        this.rating = rating;
    }

    private static void validateRating(Double rating){
        if(rating == null || rating < 0.0 || rating > 5.0){
            throw new InvalidRatingException(rating);
        }
    }
}
