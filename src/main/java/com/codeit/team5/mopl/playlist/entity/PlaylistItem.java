package com.codeit.team5.mopl.playlist.entity;

import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.global.entity.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Getter
@Entity
@Table(name = "playlist_items")
@Immutable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "createdAt", column = @Column(name = "added_at"))
public class PlaylistItem extends BaseEntity {

    @NotNull
    @Column(name = "playlist_id")
    private UUID playlistId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id")
    private Content content;

    private PlaylistItem(UUID playlistId, Content content) {
        this.playlistId = playlistId;
        this.content = content;
    }

    public static PlaylistItem of(UUID playlistId, Content content) {
        return new PlaylistItem(playlistId, content);
    }
}
