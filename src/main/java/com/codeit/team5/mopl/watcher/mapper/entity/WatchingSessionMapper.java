package com.codeit.team5.mopl.watcher.mapper.entity;

import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.watcher.constant.SortByType;
import com.codeit.team5.mopl.watcher.dto.response.WatchingSessionResponse;
import com.codeit.team5.mopl.watcher.entity.WatchingSession;
import com.codeit.team5.mopl.watcher.mapper.config.WatcherMapperConfig;
import java.util.List;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Window;

@Mapper(config = WatcherMapperConfig.class, uses = {WatcherMapper.class,
        WatcherContentMapper.class})
public interface WatchingSessionMapper {

    WatchingSessionResponse toDto(WatchingSession entity);

    default CursorResponse<WatchingSessionResponse> toCursor(Window<WatchingSession> window,
            Long totalCount, SortByType sortBy, Direction sortDirection) {
        if (window == null) {
            return null;
        }
        List<WatchingSessionResponse> data = window.getContent().stream()
                .map(this::toDto)
                .toList();
        String nextCursor = null;
        String nextIdAfter = null;
        if (window.hasNext() && !data.isEmpty()) {
            WatchingSessionResponse lastElement = data.get(data.size() - 1);
            nextCursor =
                    lastElement.createdAt() != null ? lastElement.createdAt().toString() : null;
            nextIdAfter = lastElement.id() != null ? lastElement.id().toString() : null;
        }
        return new CursorResponse<>(
                data,
                nextCursor,
                nextIdAfter,
                window.hasNext(),
                totalCount,
                sortBy.toString(),
                sortDirection.toString()
        );
    }
}
