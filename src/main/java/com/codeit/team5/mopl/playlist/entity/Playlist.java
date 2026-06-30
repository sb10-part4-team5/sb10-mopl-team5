package com.codeit.team5.mopl.playlist.entity;

import com.codeit.team5.mopl.global.entity.BaseUpdatableEntity;
import com.codeit.team5.mopl.user.entity.User;
import io.jsonwebtoken.lang.Assert;
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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "playlists")
public class Playlist extends BaseUpdatableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", nullable = false, length = Integer.MAX_VALUE)
    private String description;

    @Column(name = "subscriber_count", nullable = false)
    private Integer subscriberCount;

    @Builder(access = AccessLevel.PRIVATE)
    private Playlist(User owner, String title, String description, Integer subscriberCount) {
        Assert.hasText(title, "제목을 입력해주세요.");
        Assert.hasText(description, "상세 설명을 입력해주세요.");
        this.owner = owner;
        this.title = title;
        this.description = description;
        this.subscriberCount = subscriberCount;
    }

    public static Playlist of(User owner, String title, String description) {
        return Playlist.builder()
                .owner(owner)
                .title(title)
                .description(description)
                .subscriberCount(0)
                .build();
    }
}
