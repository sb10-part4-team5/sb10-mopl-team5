package com.codeit.team5.mopl.content.dto.external.tmdb;

import java.util.Arrays;
import java.util.Optional;

public enum TmdbTvGenre {
    ACTION_ADVENTURE(10759, "액션 & 어드벤처"),
    ANIMATION(16, "애니메이션"),
    COMEDY(35, "코미디"),
    CRIME(80, "범죄"),
    DOCUMENTARY(99, "다큐멘터리"),
    DRAMA(18, "드라마"),
    FAMILY(10751, "가족"),
    KIDS(10762, "어린이"),
    MYSTERY(9648, "미스터리"),
    NEWS(10763, "뉴스"),
    REALITY(10764, "리얼리티"),
    SCI_FI_FANTASY(10765, "SF & 판타지"),
    SOAP(10766, "연속극"),
    TALK(10767, "토크"),
    WAR_POLITICS(10768, "전쟁 & 정치"),
    WESTERN(37, "서부");

    private final long id;
    private final String label;

    TmdbTvGenre(long id, String label) {
        this.id = id;
        this.label = label;
    }

    public long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public static Optional<TmdbTvGenre> fromId(long id) {
        return Arrays.stream(values())
                .filter(g -> g.id == id)
                .findFirst();
    }
}
