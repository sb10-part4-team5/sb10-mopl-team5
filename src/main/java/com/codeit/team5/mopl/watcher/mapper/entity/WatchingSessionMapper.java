package com.codeit.team5.mopl.watcher.mapper.entity;

import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.mapper.ContentMapper;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.global.mapper.GlobalMapperConfig;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.mapper.UserMapper;
import com.codeit.team5.mopl.watcher.dto.response.WatchingSessionResponse;
import com.codeit.team5.mopl.watcher.entity.WatchingSession;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class, uses = {UserMapper.class, ContentMapper.class})
public interface WatchingSessionMapper {

    @Mapping(target = "id", source = "session.watcherId")
    @Mapping(target = "createdAt", source = "session.createdAt")
    @Mapping(target = "watcher", source = "user")
    @Mapping(target = "content", source = "content")
    WatchingSessionResponse toDto(WatchingSession session, User user, Content content);

    default CursorResponse<WatchingSessionResponse> toCursor(List<WatchingSessionResponse> data,
            boolean hasNext, Long totalCount, String sortBy, String sortDirection) {
        String nextCursor = null;
        String nextIdAfter = null;
        if (hasNext && data != null && !data.isEmpty()) {
            WatchingSessionResponse lastElement = data.get(data.size() - 1);
            nextCursor =
                    lastElement.createdAt() != null ? lastElement.createdAt().toString() : null;
            nextIdAfter = lastElement.id() != null ? lastElement.id().toString() : null;
        }
        return new CursorResponse<>(data, nextCursor, nextIdAfter, hasNext, totalCount, sortBy,
                sortDirection);
    }
}
