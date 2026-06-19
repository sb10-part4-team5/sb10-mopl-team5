package com.codeit.team5.mopl.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@EntityListeners(AuditingEntityListener.class)
@Getter
@MappedSuperclass
@NoArgsConstructor
public abstract class BaseEntity {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable=false)
    private UUID id;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public final int hashCode() {
        return getClass().hashCode();
    }
}
