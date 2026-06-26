package com.codeit.team5.mopl.follow.entity;

import com.codeit.team5.mopl.global.entity.BaseEntity;
import com.codeit.team5.mopl.user.entity.User;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "follows",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_follows_follower_id_followee_id",
        columnNames = {"follower_id", "followee_id"}
    )
)
public class Follow extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "followee_id", nullable = false)
    private User followee;

    private Follow(User follower, User followee) {
        this.follower = follower;
        this.followee = followee;
    }

    public static Follow create(User follower, User followee) {
        return new Follow(follower, followee);
    }

    public boolean isOwnedBy(UUID userId) {
        return follower.getId().equals(userId);
    }
}
