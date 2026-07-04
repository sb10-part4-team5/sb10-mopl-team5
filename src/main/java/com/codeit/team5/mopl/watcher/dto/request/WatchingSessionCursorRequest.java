package com.codeit.team5.mopl.watcher.dto.request;

import org.springframework.data.domain.Sort;
import com.codeit.team5.mopl.watcher.constant.WatcherSortByType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record WatchingSessionCursorRequest(String watcherNameLike, String cursor, String idAfter,
        @NotNull @Positive @Max(50) Integer limit, @NotNull Sort.Direction sortDirection,
        @NotNull WatcherSortByType sortBy) {

}
