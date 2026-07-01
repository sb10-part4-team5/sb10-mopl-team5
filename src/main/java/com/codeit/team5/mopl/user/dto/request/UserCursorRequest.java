package com.codeit.team5.mopl.user.dto.request;

import com.codeit.team5.mopl.user.constant.UserSortBy;
import com.codeit.team5.mopl.user.entity.UserRole;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import org.springframework.data.domain.Sort;

public record UserCursorRequest(
        String emailLike,
        UserRole roleEqual,
        Boolean isLocked,
        String cursor,
        UUID idAfter,

        @NotNull
        @Positive
        @Max(100)
        Integer limit,

        @NotNull
        Sort.Direction sortDirection,

        @NotNull
        UserSortBy sortBy
) {
}
