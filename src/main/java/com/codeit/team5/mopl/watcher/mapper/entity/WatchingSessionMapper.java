package com.codeit.team5.mopl.watcher.mapper.entity;

import java.util.List;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Window;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.global.mapper.GlobalMapperConfig;
import com.codeit.team5.mopl.user.mapper.UserSummaryMapper;
import com.codeit.team5.mopl.watcher.constant.WatcherSortByType;
import com.codeit.team5.mopl.watcher.dto.response.WatchingSessionResponse;
import com.codeit.team5.mopl.watcher.entity.WatchingSession;

@Mapper(config = GlobalMapperConfig.class,
        uses = {UserSummaryMapper.class, WatcherContentMapper.class})
public interface WatchingSessionMapper {

    WatchingSessionResponse toDto(WatchingSession entity);

    default CursorResponse<WatchingSessionResponse> toCursor(Window<WatchingSession> window,
            Long totalCount, WatcherSortByType sortBy, Direction sortDirection) {
        if (window == null || window.isEmpty()) {
            return null;
        }
        List<WatchingSessionResponse> data = window.getContent().stream().map(this::toDto).toList();
        String nextCursor = null;
        String nextIdAfter = null;
        if (window.hasNext() && !data.isEmpty()) {
            WatchingSessionResponse lastElement = data.get(data.size() - 1);
            nextCursor =
                    lastElement.createdAt() != null ? lastElement.createdAt().toString() : null;
            nextIdAfter = lastElement.id() != null ? lastElement.id().toString() : null;
        }
        return new CursorResponse<>(data, nextCursor, nextIdAfter, window.hasNext(), totalCount,
                sortBy.toString(), sortDirection.toString());
    }
}
