package com.codeit.team5.mopl.watcher.mapper.entity;

import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.watcher.dto.response.WatchingContentResponse;
import com.codeit.team5.mopl.watcher.mapper.config.WatcherMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = WatcherMapperConfig.class)
public interface WatcherContentMapper {

    WatchingContentResponse toDto(Content content);
}
