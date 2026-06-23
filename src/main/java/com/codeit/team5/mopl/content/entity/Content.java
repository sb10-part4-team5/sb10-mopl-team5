package com.codeit.team5.mopl.content.entity;

import com.codeit.team5.mopl.global.entity.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
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
}
