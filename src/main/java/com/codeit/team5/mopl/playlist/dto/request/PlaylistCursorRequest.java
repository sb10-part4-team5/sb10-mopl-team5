package com.codeit.team5.mopl.playlist.dto.request;

import java.util.UUID;
import org.springframework.data.domain.Sort;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PlaylistCursorRequest(String keywordLike,
                                    UUID ownerIdEqual,
                                    UUID subscriberIdEqual,
                                    String cursor,
                                    UUID idAfter,
                                    @NotNull @Positive Integer limit,
                                    @NotNull Sort.Direction sortDirection,
                                    @NotNull String sortBy) {

}
