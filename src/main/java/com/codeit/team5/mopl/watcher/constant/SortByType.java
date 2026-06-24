package com.codeit.team5.mopl.watcher.constant;

import com.codeit.team5.mopl.watcher.exception.WatcherErrorCode;
import com.codeit.team5.mopl.watcher.exception.WatcherException;
import java.util.Map;
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
        throw new WatcherException(WatcherErrorCode.INCORRECT_SORT_BY, Map.of("sortBy", text));
    }
}
