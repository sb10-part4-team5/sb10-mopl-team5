package com.codeit.team5.mopl.user.dto.response;

import com.codeit.team5.mopl.user.constant.UserSortBy;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;

public record UserCursorPageResponse(
        List<UserResponse> data,
        String nextCursor,
        UUID nextIdAfter,
        Integer totalCount,
        UserSortBy sortBy,
        Sort.Direction sortDirection,
        boolean hasNext
) {

}
