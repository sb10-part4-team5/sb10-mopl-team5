package com.codeit.team5.mopl.watcher.mapper;

import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.watcher.dto.WatchingSessionResponse;
import com.codeit.team5.mopl.watcher.entity.WatchingSession;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.MapperConfig;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Window;

@MapperConfig(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface WatchingSessionMapper {

    WatchingSessionResponse toDto(WatchingSession entity);

    CursorResponse<WatchingSessionResponse> toCursor(Window<WatchingSession> window);
}
