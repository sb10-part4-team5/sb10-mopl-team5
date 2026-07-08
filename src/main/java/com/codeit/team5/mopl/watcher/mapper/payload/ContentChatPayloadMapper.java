package com.codeit.team5.mopl.watcher.mapper.payload;

import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.watcher.dto.payload.ContentChatPayload;
import com.codeit.team5.mopl.watcher.dto.request.ContentChatCreatedRequest;
import com.codeit.team5.mopl.global.mapper.GlobalMapperConfig;
import com.codeit.team5.mopl.user.mapper.UserSummaryMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class, uses = {UserSummaryMapper.class})
public interface ContentChatPayloadMapper {

    @Mapping(target = "content", source = "request.content")
    ContentChatPayload toDto(User watcher, ContentChatCreatedRequest request);
}
