package com.codeit.team5.mopl.playlist.constant;

import com.codeit.team5.mopl.playlist.exception.PlaylistIncorrectSortByException;
import com.codeit.team5.mopl.playlist.exception.PlaylistSortByMismatchException;
import java.time.Instant;
import java.util.function.Function;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlaylistSortBy {
    UPDATED_AT("updatedAt", Instant::parse),
    SUBSCRIBE_COUNT("subscribeCount", Integer::parseInt),
    ;
    private final String sortByType;
    private final Function<String, ?> parser;

    public static PlaylistSortBy from(String text) {
        for (PlaylistSortBy value : PlaylistSortBy.values()) {
            if (value.getSortByType().equalsIgnoreCase(text)) {
                return value;
            }
        }
        throw new PlaylistIncorrectSortByException(text);
    }

    public Object parse(String cursor) {
        try {
            return parser.apply(cursor);
        } catch (Exception ex) {
            throw new PlaylistSortByMismatchException(sortByType, cursor);
        }

    }
}
