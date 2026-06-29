package com.codeit.team5.mopl.watcher.dto.request;

import com.codeit.team5.mopl.watcher.constant.SortByType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Sort;

public record WatchingSessionCursorRequest(String watcherNameLike,
                                           String cursor,
                                           String idAfter,
                                           @NotNull @Positive Integer limit,
                                           @NotNull Sort.Direction sortDirection,
                                           @NotNull SortByType sortBy) {

}
