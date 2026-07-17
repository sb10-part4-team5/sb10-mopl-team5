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

    // toDto(Content, ContentRatingStats) default 메서드 추가
    // 기존 MapStruct 매핑에서 watcherCount만 유지하고 나머지 stats를 캐시값으로 교체
    default ContentResponse toDto(Content content, ContentRatingStats ratingStats) {
        ContentResponse base = toDto(content);
        return new ContentResponse(
                base.id(), base.type(), base.title(), base.description(),
                base.thumbnailUrl(), base.tags(),
                ratingStats.averageRating(), ratingStats.reviewCount(), base.watcherCount()
        );
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
        String direction = sortDirection == Direction.ASC ? "ASCENDING" : "DESCENDING";
        return new CursorResponse<>(data, nextCursor, nextIdAfter, hasNext, totalCount,
                sortBy.getValue(), direction);
    }
}
