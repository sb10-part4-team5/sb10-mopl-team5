package com.codeit.team5.mopl.playlist.entity;

import com.codeit.team5.mopl.global.entity.BaseUpdatableEntity;
import com.codeit.team5.mopl.user.entity.User;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@Entity
@Table(name = "playlists", schema = "public")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt",
                column = @Column(nullable = false)),
        @AttributeOverride(name = "updatedAt",
                column = @Column(nullable = false))})
public class Playlist extends BaseUpdatableEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Size(max = 255)
    @NotNull
    @Column(name = "title", nullable = false)
    private String title;

    @NotNull
    @Column(name = "description", nullable = false, length = Integer.MAX_VALUE)
    private String description;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "subscriber_count", nullable = false)
    private Integer subscriberCount;


}
