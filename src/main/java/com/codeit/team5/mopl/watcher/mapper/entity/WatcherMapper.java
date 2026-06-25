package com.codeit.team5.mopl.watcher.mapper.entity;

import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.watcher.dto.response.WatcherResponse;
import com.codeit.team5.mopl.watcher.mapper.config.WatcherMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = WatcherMapperConfig.class)
public interface WatcherMapper {

    WatcherResponse toDto(User user);
}
