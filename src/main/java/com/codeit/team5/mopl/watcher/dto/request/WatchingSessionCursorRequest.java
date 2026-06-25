package com.codeit.team5.mopl.watcher.dto.request;

import com.codeit.team5.mopl.watcher.constant.SortByType;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Sort;

public record WatchingSessionCursorRequest(String watcherNameLike,
                                           String cursor,
                                           String idAfter,
                                           @NotNull int limit,
                                           @NotNull Sort.Direction sortDirection,
                                           @NotNull SortByType sortBy) {

}
