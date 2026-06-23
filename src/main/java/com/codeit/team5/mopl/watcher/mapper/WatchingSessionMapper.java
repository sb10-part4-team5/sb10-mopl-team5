package com.codeit.team5.mopl.watcher.mapper;

import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.global.mapper.GlobalMapperConfig;
import com.codeit.team5.mopl.watcher.dto.WatchingSessionResponse;
import com.codeit.team5.mopl.watcher.entity.WatchingSession;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Window;

@Mapper(config = GlobalMapperConfig.class)
public interface WatchingSessionMapper {

    WatchingSessionResponse toDto(WatchingSession entity);
    CursorResponse<WatchingSessionResponse> toCursor(Window<WatchingSession> window);
}
