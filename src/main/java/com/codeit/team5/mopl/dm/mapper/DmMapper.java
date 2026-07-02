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

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        uses = UserMapper.class
)
public interface DmMapper {

    @Mapping(target = "conversationId", source = "conversation.id")
    DirectMessageResponse toResponse(DirectMessage message);

    default CursorResponse<DirectMessageResponse> toDirectMessageCursor(List<DirectMessage> page, boolean hasNext,
            Direction sortDirection) {
        List<DirectMessageResponse> data = page.stream().map(this::toResponse).toList();
        DirectMessage last = page.isEmpty() ? null : page.get(page.size() - 1);
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
