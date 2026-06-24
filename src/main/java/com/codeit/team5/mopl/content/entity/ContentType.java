package com.codeit.team5.mopl.content.entity;

import com.codeit.team5.mopl.content.exception.InvalidContentTypeException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ContentType {
    MOVIE("movie"),
    TV_SERIES("tvSeries"),
    SPORT("sport");

    private final String response;

    ContentType(String response) {
        this.response = response;
    }

    @JsonValue
    public String getResponse() {
        return response;
    }

    @JsonCreator
    public static ContentType from(String value) {
        for (ContentType type : values()) {
            if (type.response.equals(value)) {
                return type;
            }
        }
        throw new InvalidContentTypeException(value);
    }
}
