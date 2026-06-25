package com.codeit.team5.mopl.global.web.ws.stomp.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StompConstants {
    public static final String PUB_PREFIX = "/pub";
    public static final String SUB_PREFIX = "/sub";

    public static final String PUB_WATCHING_CONTENT_CHAT = "/contents/{id}/chat";
    public static final String SUB_WATCHING_CONTENT_CHAT = SUB_PREFIX + "/contents/{id}/chat";
    public static final String SUB_WATCHING_CONTENT = SUB_PREFIX + "/contents/{id}/watch";
}
