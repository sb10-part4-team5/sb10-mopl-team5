package com.codeit.team5.mopl.content.mapper;

import com.codeit.team5.mopl.content.dto.response.ContentResponse;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentSortByType;
import com.codeit.team5.mopl.content.entity.ContentStats;
import com.codeit.team5.mopl.content.entity.ContentTag;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Sort.Direction;

@Mapper(componentModel = "spring", uses = {ContentUtilsMapper.class})
public interface ContentMapper {

    @Mapping(target = "tags", source = "contentTags", qualifiedByName = "toTagNames")
    @Mapping(target = "averageRating", source = "stats", qualifiedByName = "toAverageRating")
    @Mapping(target = "reviewCount", source = "stats.reviewCount")
    @Mapping(target = "watcherCount", source = "stats.watcherCount")
    @Mapping(target = "thumbnailUrl", source = "thumbnail.url")
    @Mapping(target = "thumbnailUploadStatus", source = "thumbnail.uploadStatus")
    ContentResponse toDto(Content content);

    default List<String> toTagNames(Set<ContentTag> contentTags) {
        if (contentTags == null) return Collections.emptyList();
        return contentTags.stream()
                .map(ct -> ct.getTag().getName())
                .sorted()
                .toList();
    }

    default double toAverageRating(ContentStats stats) {
        if (stats == null) return 0.0;
        return stats.getAverageRating();
    }

    default CursorResponse<ContentResponse> toCursor(List<Content> page, boolean hasNext,
            long totalCount, ContentSortByType sortBy, Direction sortDirection) {
        String nextCursor = null;
        String nextIdAfter = null;
        if (hasNext && !page.isEmpty()) {
            Content last = page.get(page.size() - 1);
            nextCursor = switch (sortBy) {
                case CREATED_AT -> last.getCreatedAt().toString();
                case WATCHER_COUNT -> String.valueOf(last.getStats().getWatcherCount());
                case RATE -> String.valueOf(last.getStats().getAverageRating());
            };
            nextIdAfter = last.getId().toString();
        }
        List<ContentResponse> data = page.stream().map(this::toDto).toList();
        return new CursorResponse<>(data, nextCursor, nextIdAfter, hasNext, totalCount,
                sortBy.getValue(), sortDirection.toString());
    }
}
