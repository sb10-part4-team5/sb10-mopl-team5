package com.codeit.team5.mopl.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "content_stats")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentStats {

    @Id
    private UUID contentId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id")
    private Content content;

    @Column(nullable = false)
    private int reviewCount;

    @Column(nullable = false)
    private double ratingSum;

    @Column(nullable = false)
    private int watcherCount;

    @Column(nullable = false)
    private Instant updatedAt;
}
