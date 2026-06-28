package com.codeit.team5.mopl.content.dto.request;

import com.codeit.team5.mopl.content.entity.ContentSortByType;
import com.codeit.team5.mopl.content.entity.ContentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.data.domain.Sort;

public record ContentCursorRequest(
        ContentType typeEqual,

        String keywordLike,

        List<String> tagsIn,

        String cursor,

        String idAfter,

        @NotNull
        @Positive
        Integer limit,

        @NotNull
        Sort.Direction sortDirection,

        @NotNull
        ContentSortByType sortBy
) {
}
