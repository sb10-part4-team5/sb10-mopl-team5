package com.codeit.team5.mopl.user.dto.request;

import com.codeit.team5.mopl.user.constant.UserSortBy;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import org.springframework.data.domain.Sort;

public record UserCursorRequest(
        @NotNull
        UserSortBy sortBy,

        @NotNull
        Sort.Direction sortDirection,

        String cursor,

        UUID cursorId,

        @NotNull
        @Positive
        Integer limit
) {

}
