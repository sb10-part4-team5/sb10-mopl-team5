package com.codeit.team5.mopl.content.entity;

import com.codeit.team5.mopl.global.entity.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "contents")
public class Content extends BaseUpdatableEntity {

    @Size(max = 20)
    @NotNull
    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Size(max = 500)
    @NotNull
    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @Size(max = 512)
    @Column(name = "thumbnail_url", length = 512)
    private String thumbnailUrl;

    @Column(name = "released_at")
    private Instant releasedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private Map<String, Object> metadata;

    @Size(max = 20)
    @NotNull
    @Column(name = "source", nullable = false, length = 20)
    private String source;

    @Size(max = 100)
    @NotNull
    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
