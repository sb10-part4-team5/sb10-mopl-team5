package com.codeit.team5.mopl.review.entity;

import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.global.entity.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID authorId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    @Column(nullable = false)
    private Double rating;

    public static Review of(Content content, UUID authorId, String text, Double rating) {
        return new Review(content, authorId, text, rating);
    }

    private Review(Content content, UUID authorId, String text, Double rating) {
        this.content = content;
        this.authorId = authorId;
        this.text = text;
        this.rating = rating;
    }

    public UUID getContentId() {
        return content.getId();
    }

    public void update(String text, Double rating) {
        if (text != null) {
            this.text = text;
        }
        if(rating != null){
            this.rating = rating;
        }
    }
}
