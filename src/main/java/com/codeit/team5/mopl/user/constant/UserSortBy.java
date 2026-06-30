package com.codeit.team5.mopl.user.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserSortBy {
    CREATED_AT("createdAt"),
    ;

    private final String value;
}
