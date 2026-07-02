package com.codeit.team5.mopl.dm.mapper;

import com.codeit.team5.mopl.dm.dto.response.ConversationResponse;
import com.codeit.team5.mopl.dm.entity.Conversation;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Sort.Direction;

@Mapper(componentModel = "spring")
public interface ConversationMapper {

    default CursorResponse<ConversationResponse> toCursor(
            List<ConversationResponse> data,
            Conversation last,
            boolean hasNext,
            Direction sortDirection
    ) {
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
