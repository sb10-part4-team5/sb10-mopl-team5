package com.codeit.team5.mopl.dm.dto.request;

import jakarta.validation.constraints.AssertTrue;
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

    @AssertTrue(message = "cursor와 idAfter는 함께 제공되어야 합니다")
    private boolean isCursorPairValid() {
        return (cursor == null) == (idAfter == null);
    }
}
