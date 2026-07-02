package com.codeit.team5.mopl.dm.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Sort;

public record ConversationCursorRequest(
        String keywordLike,

        Instant cursor,

        UUID idAfter,

        @NotNull
        @Positive
        @Max(100)
        Integer limit,

        @NotNull
        Sort.Direction sortDirection
) {
}
