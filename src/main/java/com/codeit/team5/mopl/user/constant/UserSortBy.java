package com.codeit.team5.mopl.user.constant;

import com.codeit.team5.mopl.user.exception.InvalidUserSortByException;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserSortBy {
    NAME("name"),
    EMAIL("email"),
    CREATED_AT("createdAt"),
    LOCKED("isLocked"),
    ROLE("role")
    ;

    private final String value;

    public static UserSortBy from(String value) {
        return Arrays.stream(values())
                .filter(sortBy -> sortBy.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new InvalidUserSortByException(value));
    }
}
