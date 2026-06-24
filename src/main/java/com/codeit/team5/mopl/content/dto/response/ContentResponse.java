package com.codeit.team5.mopl.content.dto.response;

import com.codeit.team5.mopl.binarycontent.entity.BinaryContentUploadStatus;
import com.codeit.team5.mopl.content.entity.ContentType;
import java.util.List;
import java.util.UUID;

public record ContentResponse(
        UUID id,
        ContentType type,
        String title,
        String description,
        String thumbnailUrl,
        BinaryContentUploadStatus thumbnailUploadStatus,
        List<String> tags,
        double averageRating,
        int reviewCount,
        long watcherCount
) {
}
