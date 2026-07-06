package com.codeit.team5.mopl.content.entity;

import com.codeit.team5.mopl.content.exception.ContentIncorrectSortByException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContentSortByType {
    CREATED_AT("createdAt"),
    WATCHER_COUNT("watcherCount"),
    RATE("rate")
    ;
    private final String value;

    public static ContentSortByType from(String text) {
        for (ContentSortByType type : ContentSortByType.values()) {
            if (type.getValue().equalsIgnoreCase(text)) {
                return type;
            }
        }
        throw new ContentIncorrectSortByException(text);
    }
}
