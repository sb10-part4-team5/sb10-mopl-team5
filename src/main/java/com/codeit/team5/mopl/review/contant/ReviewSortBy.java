package com.codeit.team5.mopl.review.contant;

import com.codeit.team5.mopl.review.exception.InvalidReviewSortByException;
import com.codeit.team5.mopl.review.exception.ReviewSortByMismatchException;
import java.time.Instant;
import java.util.function.Function;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReviewSortBy {
    CREATED_AT("createdAt", Instant::parse),
    RATING("rating", Double::parseDouble),
    ;

    private final String sortByType;
    private final Function<String, ?> parser;

    public static ReviewSortBy from(String string) {
        for (ReviewSortBy value : ReviewSortBy.values()) {
            if (string.equalsIgnoreCase(value.sortByType)) {
                return value;
            }
        }
        throw new InvalidReviewSortByException(string);
    }

    public Object parse(String cursor){
        try{
            return parser.apply(cursor);
        } catch (Exception e){
            throw new ReviewSortByMismatchException(sortByType, cursor);
        }
    }
}
