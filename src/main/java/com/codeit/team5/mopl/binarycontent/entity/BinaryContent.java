package com.codeit.team5.mopl.binarycontent.entity;

import com.codeit.team5.mopl.global.entity.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "binary_contents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BinaryContent extends BaseUpdatableEntity {

    @Column(nullable = false, length = 512)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status", nullable = false, length = 20)
    private BinaryContentUploadStatus uploadStatus;

    public static BinaryContent pending(String url) {
        BinaryContent bc = new BinaryContent();
        bc.url = url;
        bc.uploadStatus = BinaryContentUploadStatus.PENDING;
        return bc;
    }

    public void updateUploadStatus(BinaryContentUploadStatus status) {
        this.uploadStatus = status;
    }
}
