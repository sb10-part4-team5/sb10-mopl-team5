package com.codeit.team5.mopl.subscription.entity;

import org.hibernate.annotations.Immutable;
import com.codeit.team5.mopl.global.entity.BaseEntity;
import com.codeit.team5.mopl.playlist.entity.Playlist;
import com.codeit.team5.mopl.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Immutable
@Entity
@Table(name = "playlist_subscriptions")
public class Subscription extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id")
    private Playlist playlist;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscriber_id")
    private User subscriber;

    private Subscription(Playlist playlist, User subscriber) {
        this.playlist = playlist;
        this.subscriber = subscriber;
    }

    public static Subscription of(Playlist playlist, User subscriber) {
        return new Subscription(playlist, subscriber);
    }
}
