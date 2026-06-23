package com.codeit.team5.mopl.content.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "content_tags")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentTag {

    @EmbeddedId
    private ContentTagId id;

    @MapsId("contentId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id")
    private Content content;

    @MapsId("tagId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id")
    private Tag tag;

    public ContentTag(Content content, Tag tag) {
        this.id = new ContentTagId(content.getId(), tag.getId());
        this.content = content;
        this.tag = tag;
    }

    @Embeddable
    @Getter
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ContentTagId implements Serializable {

        private UUID contentId;
        private UUID tagId;

        public ContentTagId(UUID contentId, UUID tagId) {
            this.contentId = contentId;
            this.tagId = tagId;
        }
    }
}
