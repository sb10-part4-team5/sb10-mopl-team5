package com.codeit.team5.mopl.global.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record CursorResponse<T>(List<T> data,
                                String nextCursor,
                                String nextIdAfter,
                                boolean hasNext,
                                long totalCount,
                                String sortBy,
    String sortDirection) {

    @Override
    public String sortDirection() {
        if (sortDirection == null) {
            return null;
        }
        if (sortDirection.equalsIgnoreCase("ASC") || sortDirection.equalsIgnoreCase("ASCENDING")) {
            return "ASCENDING";
        }
        if (sortDirection.equalsIgnoreCase("DESC") || sortDirection.equalsIgnoreCase("DESCENDING")) {
            return "DESCENDING";
        }
        return sortDirection;
    }
}
