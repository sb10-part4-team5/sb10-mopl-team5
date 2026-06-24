package com.codeit.team5.mopl.content.entity;

import com.codeit.team5.mopl.binarycontent.entity.BinaryContentUploadStatus;
import com.codeit.team5.mopl.content.exception.InvalidContentSourceException;
import com.codeit.team5.mopl.global.entity.BaseUpdatableEntity;
import com.codeit.team5.mopl.tag.entity.Tag;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
        name = "contents",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_contents_source_external_id",
                        columnNames = {"source", "external_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Content extends BaseUpdatableEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentType type;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail_url", length = 512)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "thumbnail_upload_status", length = 20)
    private BinaryContentUploadStatus thumbnailUploadStatus;

    @Column(name = "released_at")
    private Instant releasedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private String metadata;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentSource source;

    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    @OneToMany(mappedBy = "content", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContentTag> contentTags = new ArrayList<>();

    public static Content createByAdmin(ContentType type, String title, String description) {
        Content content = new Content();
        content.type = type;
        content.title = title;
        content.description = description;
        content.source = ContentSource.ADMIN;
        content.externalId = UUID.randomUUID().toString();
        return content;
    }

    public static Content createByExternalSource(ContentType type, String title, String description,
            ContentSource source, String externalId, Instant releasedAt, String metadata) {
        if (source == null || source == ContentSource.ADMIN) {
            throw new InvalidContentSourceException("외부 소스 생성에 ADMIN 소스는 사용할 수 없습니다.");
        }
        if (externalId == null || externalId.isBlank()) {
            throw new InvalidContentSourceException("외부 소스 콘텐츠는 externalId가 필수입니다.");
        }
        Content content = new Content();
        content.type = type;
        content.title = title;
        content.description = description;
        content.source = source;
        content.externalId = externalId;
        content.releasedAt = releasedAt;
        content.metadata = metadata;
        return content;
    }

    public void initThumbnail(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
        this.thumbnailUploadStatus = BinaryContentUploadStatus.PENDING;
    }

    public void completeThumbnailUpload() {
        this.thumbnailUploadStatus = BinaryContentUploadStatus.COMPLETED;
    }

    public void failThumbnailUpload() {
        this.thumbnailUploadStatus = BinaryContentUploadStatus.FAILED;
    }

    public void addTag(Tag tag) {
        if (tag == null) {
            return;
        }

        boolean alreadyExists = this.contentTags.stream()
                .anyMatch(ct -> {
                    Tag existing = ct.getTag();
                    if (existing.getId() != null && tag.getId() != null) {
                        return existing.getId().equals(tag.getId());
                    }
                    return existing.getName().equals(tag.getName());
                });

        if (!alreadyExists) {
            contentTags.add(ContentTag.create(this, tag));
        }
    }
}
