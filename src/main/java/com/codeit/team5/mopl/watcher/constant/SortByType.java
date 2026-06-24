package com.codeit.team5.mopl.watcher.constant;

import com.codeit.team5.mopl.watcher.exception.WatchingSessionIncorrectSortByException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SortByType {
    CREATED_AT("createdAt"),
    ;
    private final String value;

    public static SortByType from(String text) {
        for (SortByType type : SortByType.values()) {
            if (type.getValue().equalsIgnoreCase(text)) {
                return type;
            }
        }
        throw new WatchingSessionIncorrectSortByException(text);
    }
}
