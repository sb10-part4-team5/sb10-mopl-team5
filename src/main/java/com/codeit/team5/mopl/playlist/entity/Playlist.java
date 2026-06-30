package com.codeit.team5.mopl.playlist.entity;

import com.codeit.team5.mopl.global.entity.BaseUpdatableEntity;
import com.codeit.team5.mopl.user.entity.User;
import io.jsonwebtoken.lang.Assert;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

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

    public void updateTitle(String title) {
        updateIfChanged(this.title, title, value -> this.title = value);
    }

    public void updateDescription(String description) {
        updateIfChanged(this.description, description, value -> this.description = value);
    }

    private void updateIfChanged(String oldValue, String newValue, Consumer<String> update) {
        if (!StringUtils.hasText(newValue) || oldValue.equals(newValue)) {
            return;
        }
        update.accept(newValue);
    }
}
