package com.codeit.team5.mopl.global.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import lombok.Builder;
import org.springframework.data.domain.Sort.Direction;

@Builder
public record CursorResponse<T>(List<T> data,
                                String nextCursor,
                                String nextIdAfter,
                                boolean hasNext,
                                long totalCount,
                                String sortBy,
                                String sortDirection) {

    // 마지막 엔티티에서 다음 커서를 추출해 커서 응답을 조립
    public static <T, E> CursorResponse<T> of(
            List<T> data, E lastEntity, boolean hasNext, long totalCount,
            Function<E, Instant> cursorExtractor, Function<E, UUID> idExtractor,
            Direction direction, String sortBy) {
        String nextCursor = null;
        String nextIdAfter = null;
        if (hasNext && lastEntity != null) {
            nextCursor = cursorExtractor.apply(lastEntity).toString();
            nextIdAfter = idExtractor.apply(lastEntity).toString();
        }
        String sortDirection = direction == Direction.ASC ? "ASCENDING" : "DESCENDING";
        return new CursorResponse<>(data, nextCursor, nextIdAfter, hasNext, totalCount, sortBy, sortDirection);
    }
}
