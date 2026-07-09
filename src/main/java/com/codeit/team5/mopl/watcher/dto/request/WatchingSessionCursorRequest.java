package com.codeit.team5.mopl.watcher.dto.request;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record WatchingSessionCursorRequest(String watcherNameLike, Instant cursor, UUID idAfter,
                @NotNull @Positive @Max(100) Integer limit, @NotNull Sort.Direction sortDirection,
                @NotNull @Pattern(regexp = "createdAt") String sortBy) {

}
