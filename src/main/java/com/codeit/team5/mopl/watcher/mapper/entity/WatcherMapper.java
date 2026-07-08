package com.codeit.team5.mopl.watcher.mapper.entity;

import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.watcher.dto.response.WatcherResponse;
import com.codeit.team5.mopl.watcher.mapper.config.WatcherMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = WatcherMapperConfig.class)
public interface WatcherMapper {

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "profileImageUrl", source = "profileImage.url")
    WatcherResponse toDto(User user);
}
