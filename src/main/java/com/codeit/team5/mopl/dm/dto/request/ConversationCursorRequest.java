package com.codeit.team5.mopl.dm.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Sort;

public record ConversationCursorRequest(
        String keywordLike,

        String cursor,

        String idAfter,

        @NotNull
        @Positive
        @Max(100)
        Integer limit,

        @NotNull
        Sort.Direction sortDirection
) {
}
