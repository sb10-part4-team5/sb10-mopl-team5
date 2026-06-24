package com.codeit.team5.mopl.content.mapper;

import com.codeit.team5.mopl.content.dto.response.ContentResponse;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentStats;
import com.codeit.team5.mopl.content.entity.ContentTag;
import java.util.Collections;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ContentMapper {

    @Mapping(target = "tags", source = "contentTags")
    @Mapping(target = "averageRating", source = "stats")
    @Mapping(target = "reviewCount", source = "stats.reviewCount")
    @Mapping(target = "watcherCount", source = "stats.watcherCount")
    @Mapping(target = "thumbnailUploadStatus", source = "content.thumbnailUploadStatus")
    ContentResponse toDto(Content content, List<ContentTag> contentTags, ContentStats stats);

    default List<String> toTagNames(List<ContentTag> contentTags) {
        if (contentTags == null) return Collections.emptyList();
        return contentTags.stream()
                .map(ct -> ct.getTag().getName())
                .toList();
    }

    default double toAverageRating(ContentStats stats) {
        if (stats == null || stats.getReviewCount() == 0) return 0.0;
        return stats.getRatingSum() / stats.getReviewCount();
    }
}
