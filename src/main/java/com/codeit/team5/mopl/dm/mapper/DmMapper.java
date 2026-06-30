package com.codeit.team5.mopl.dm.mapper;

import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.dm.entity.DirectMessage;
import com.codeit.team5.mopl.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        uses = UserMapper.class
)
public interface DmMapper {

    @Mapping(target = "conversationId", source = "conversation.id")
    DirectMessageResponse toResponse(DirectMessage message);
}
