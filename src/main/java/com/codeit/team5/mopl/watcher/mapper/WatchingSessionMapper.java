package com.codeit.team5.mopl.watcher.mapper;

import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.watcher.constant.SortByType;
import com.codeit.team5.mopl.watcher.dto.WatchingSessionResponse;
import com.codeit.team5.mopl.watcher.entity.WatchingSession;
import java.util.List;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Window;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface WatchingSessionMapper {

    WatchingSessionResponse toDto(WatchingSession entity);

    default CursorResponse<WatchingSessionResponse> toCursor(Window<WatchingSession> window,
            SortByType sortBy, Direction sortDirection) {
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
                0L,
                sortBy.toString(),
                sortDirection.toString()
        );
    }
}
