package com.codeit.team5.mopl.watcher.mapper.payload;

import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.watcher.dto.payload.ContentChatPayload;
import com.codeit.team5.mopl.watcher.dto.request.ContentChatCreatedRequest;
import com.codeit.team5.mopl.watcher.mapper.config.WatcherMapperConfig;
import com.codeit.team5.mopl.watcher.mapper.entity.WatcherMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = WatcherMapperConfig.class, uses = {WatcherMapper.class})
public interface ContentChatPayloadMapper {

    @Mapping(target = "content", source = "request.content")
    ContentChatPayload toDto(User watcher, ContentChatCreatedRequest request);
}
