package com.codeit.team5.mopl.playlist.dto;

import com.codeit.team5.mopl.playlist.constant.PlaylistSortBy;
import org.springframework.data.domain.Sort;
import java.util.UUID;
import lombok.Builder;

@Builder
public record PlaylistCursorCommand(String keywordLike, UUID ownerIdEqual, UUID subscriberIdEqual,
        Object cursor, UUID idAfter, Integer limit, Sort.Direction sortDirection,
        PlaylistSortBy sortBy) {
}
