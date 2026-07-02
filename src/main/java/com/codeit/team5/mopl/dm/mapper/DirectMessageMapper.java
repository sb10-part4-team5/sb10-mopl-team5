package com.codeit.team5.mopl.dm.mapper;

import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.dm.entity.DirectMessage;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.user.mapper.UserMapper;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Window;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        uses = UserMapper.class
)
public interface DirectMessageMapper {

    @Mapping(target = "conversationId", source = "conversation.id")
    DirectMessageResponse toResponse(DirectMessage message);

    default CursorResponse<DirectMessageResponse> toCursor(
            Window<DirectMessage> window,
            Direction sortDirection
    ) {
        List<DirectMessage> content = window.getContent();
        List<DirectMessageResponse> data = content.stream().map(this::toResponse).toList();
        DirectMessage last = content.isEmpty() ? null : content.get(content.size() - 1);
        boolean hasNext = window.hasNext();
        String nextCursor = null;
        String nextIdAfter = null;
        if (hasNext && last != null) {
            nextCursor = last.getCreatedAt().toString();
            nextIdAfter = last.getId().toString();
        }
        String direction = sortDirection == Direction.ASC ? "ASCENDING" : "DESCENDING";
        return new CursorResponse<>(
                data,
                nextCursor,
                nextIdAfter,
                hasNext,
                0L,
                "createdAt",
                direction
        );
    }
}
