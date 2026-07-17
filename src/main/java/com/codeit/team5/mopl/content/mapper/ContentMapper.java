package com.codeit.team5.mopl.content.mapper;

import com.codeit.team5.mopl.content.dto.response.ContentResponse;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentSortByType;
import com.codeit.team5.mopl.content.mapper.util.ContentUtilsMapper;
import com.codeit.team5.mopl.content.store.ContentRatingStats;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import java.util.List;
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
    ContentResponse toDto(Content content);

    @Mapping(target = "tags", source = "content.contentTags", qualifiedByName = "toTagNames")
    @Mapping(target = "thumbnailUrl", source = "content.thumbnail.url")
    @Mapping(target = "watcherCount", source = "content.stats.watcherCount")
    @Mapping(target = "averageRating", source = "ratingStats.averageRating")
    @Mapping(target = "reviewCount", source = "ratingStats.reviewCount")
    // 이름이 일치하는 id, type, title, description 필드는 content 객체에서 자동으로 매핑됩니다.
    // watcherCount는 db 내 content.stat에서, rating,reviewCount는 캐싱된 ratingStars 에서 가져와 조합합니다.
    ContentResponse toDto(Content content, ContentRatingStats ratingStats);

    default CursorResponse<ContentResponse> toCursor(List<Content> page, boolean hasNext,
        long totalCount, ContentSortByType sortBy, Direction sortDirection) {
        List<ContentResponse> data = page.stream().map(this::toDto).toList();
        return toCursor(page, data, hasNext, totalCount, sortBy, sortDirection);
    }

    default CursorResponse<ContentResponse> toCursor(List<Content> page, List<ContentResponse> data,
            boolean hasNext, long totalCount, ContentSortByType sortBy, Direction sortDirection) {
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
        String direction = sortDirection == Direction.ASC ? "ASCENDING" : "DESCENDING";
        return new CursorResponse<>(data, nextCursor, nextIdAfter, hasNext, totalCount,
                sortBy.getValue(), direction);
    }
}
