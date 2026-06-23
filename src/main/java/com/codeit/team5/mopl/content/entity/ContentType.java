package com.codeit.team5.mopl.content.entity;

public enum ContentType {
    MOVIE("movie"),
    TV_SERIES("tvSeries"),
    SPORT("sport");

    private final String response;

    ContentType(String response) {
        this.response = response;
    }
}
