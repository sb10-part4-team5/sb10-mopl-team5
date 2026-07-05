package com.codeit.team5.mopl.review.contant;

import com.codeit.team5.mopl.review.exception.InvalidReviewSortByException;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReviewSortBy {
    CREATED_AT("createdAt"),
    RATING("rating"),
    ;

    private final String value;

    public static ReviewSortBy from(String value) {
        return Arrays.stream(values())
            .filter(sortBy -> sortBy.value.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new InvalidReviewSortByException(value));
    }
}
