package com.codeit.team5.mopl.watcher.constant;

import com.codeit.team5.mopl.watcher.exception.WatchingSessionIncorrectSortByException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WatcherSortByType {
    CREATED_AT("createdAt"),;

    private final String value;

    public static WatcherSortByType from(String text) {
        for (WatcherSortByType type : WatcherSortByType.values()) {
            if (type.getValue().equalsIgnoreCase(text)) {
                return type;
            }
        }
        throw new WatchingSessionIncorrectSortByException(text);
    }
}
