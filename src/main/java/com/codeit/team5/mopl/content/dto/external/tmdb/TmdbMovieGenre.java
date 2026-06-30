package com.codeit.team5.mopl.content.dto.external.tmdb;

import java.util.Arrays;
import java.util.Optional;

public enum TmdbMovieGenre {
    ACTION(28, "액션"),
    ADVENTURE(12, "어드벤처"),
    ANIMATION(16, "애니메이션"),
    COMEDY(35, "코미디"),
    CRIME(80, "범죄"),
    DOCUMENTARY(99, "다큐멘터리"),
    DRAMA(18, "드라마"),
    FAMILY(10751, "가족"),
    FANTASY(14, "판타지"),
    HISTORY(36, "역사"),
    HORROR(27, "공포"),
    MUSIC(10402, "음악"),
    MYSTERY(9648, "미스터리"),
    ROMANCE(10749, "로맨스"),
    SCIENCE_FICTION(878, "SF"),
    TV_MOVIE(10770, "TV 영화"),
    THRILLER(53, "스릴러"),
    WAR(10752, "전쟁"),
    WESTERN(37, "서부");

    private final long id;
    private final String label;

    TmdbMovieGenre(long id, String label) {
        this.id = id;
        this.label = label;
    }

    public long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public static Optional<TmdbMovieGenre> fromId(long id) {
        return Arrays.stream(values())
                .filter(g -> g.id == id)
                .findFirst();
    }
}
