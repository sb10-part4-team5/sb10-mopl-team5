package com.codeit.team5.mopl.follow.entity;

import com.codeit.team5.mopl.follow.exception.FollowForbiddenException;
import com.codeit.team5.mopl.follow.exception.SelfFollowException;
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
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Immutable;

@Entity
@Getter
@Immutable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Check(name = "ck_follow_self", constraints = "follower_id <> followee_id")
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
        if (follower.equals(followee)) {
            throw new SelfFollowException(follower.getId());
        }
        return new Follow(follower, followee);
    }

    private boolean isOwnedBy(UUID userId) {
        return follower.getId().equals(userId);
    }

    public void validateOwnedBy(UUID userId) {
        if (!isOwnedBy(userId)) {
            throw new FollowForbiddenException(getId(), userId);
        }
    }
}
